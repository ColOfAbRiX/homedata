package com.colofabrix.scala.tado4s

import cats.effect.Async
import cats.effect.std.AtomicCell
import cats.implicits.given
import com.colofabrix.scala.tado4s.api.*
import com.colofabrix.scala.tado4s.Tado4sClient.*
import fs2.io.net.Network
import io.odin.*
import io.odin.formatter.Formatter
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import org.http4s.*
import org.http4s.circe.CirceEntityDecoder.*
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.headers.Authorization
import org.http4s.Method.*

/**
 * Tado Client for Scala
 */
final class Tado4sClient[F[_]: Async](
  httpClient: Client[F],
  config: TadoConfig,
  atomicState: AtomicCell[F, TadoClientState[F]],
) extends Http4sClientDsl[F]:

  private val logger: Logger[F] = consoleLogger(formatter = Formatter.colorful)

  /**
   * Logs into the Tado service
   */
  def login(username: String, password: String): F[Unit] =
    setCredentials(username, password) >> loginRequest()

  /**
   * Logs out the Tado service
   */
  def logout(): F[Unit] =
    clearCredentials() >>
    clearAuthToken() >>
    clearAuthenticatedClient()

  /**
   * Information about the Tado account
   */
  def getAccountInfo(): F[AccountResponse] =
    useAuthClient: client =>
      val request = GET(config.apiBase / "me")
      client.expect[AccountResponse](request)

  /**
   * Information about a specific Home
   */
  def getHomeDetails(homeId: Int): F[HomeResponse] =
    useAuthClient: client =>
      val request = GET(config.apiBase / "homes" / homeId)
      client.expect[HomeResponse](request)

  /**
   * Information about the zones of a specific Home
   */
  def getHomeZones(homeId: Int): F[Vector[HomeZonesResponse]] =
    useAuthClient: client =>
      val request = GET(config.apiBase / "homes" / homeId / "zones")
      client.expect[Vector[HomeZonesResponse]](request)

  /**
   * Information the state of a Home
   */
  def getHomeState(homeId: Int): F[HomeStateResponse] =
    useAuthClient: client =>
      val request = GET(config.apiBase / "homes" / homeId / "state")
      client.expect[HomeStateResponse](request)

  /**
   * State of a specific Zone in a specific Home
   */
  def getZoneState(homeId: Int, zoneId: Int): F[ZoneStateResponse] =
    useAuthClient: client =>
      val request = GET(config.apiBase / "homes" / homeId / "zones" / zoneId / "state")
      client.expect[ZoneStateResponse](request)

  /**
   * The weather reported at the house
   */
  def getHomeWeather(homeId: Int): F[WeatherResponse] =
    useAuthClient: client =>
      val request = GET(config.apiBase / "homes" / homeId / "weather")
      client.expect[WeatherResponse](request)

  /**
   * The a daily reportfor a specific house and zone
   */
  def getZoneDayReport(homeId: Int, zoneId: Int, date: LocalDate): F[DayReportResponse] =
    useAuthClient: client =>
      val url      = config.apiBase / "homes" / homeId / "zones" / zoneId / "dayReport"
      val queryUrl = url.withQueryParam("date", date.toString())
      client.expect[DayReportResponse](GET(queryUrl))

  private def loginRequest(): F[Unit] =
    getCredentials().flatMap: credentials =>
      val requestBody =
        UrlForm(
          "client_id"     -> "tado-web-app",
          "grant_type"    -> "password",
          "scope"         -> "home.user",
          "username"      -> credentials.username,
          "password"      -> credentials.password,
          "client_secret" -> config.clientSecret,
        )

      val postRequest = POST(requestBody, config.apiAuth / "oauth" / "token")

      httpClient
        .expect[AuthResponse](postRequest)
        .flatMap { authResponse =>
          val expiry    = OffsetDateTime.now().plus(authResponse.expires_in.toLong, ChronoUnit.SECONDS)
          val authToken = TadoAuthToken(authResponse.access_token, expiry)
          setAuthToken(authToken)
        }
        .onError { error =>
          logger.error("Error while logging in", error)
        }

  private def refreshTokenRequest(): F[Unit] =
    val requestBody =
      UrlForm(
        "grant_type"    -> "refresh_token",
        "refresh_token" -> "def",
        "client_id"     -> "tado-web-app",
        "scope"         -> "home.user",
        "client_secret" -> config.clientSecret,
      )

    val postRequest = POST(requestBody, config.apiAuth)

    httpClient
      .expect[AuthResponse](postRequest)
      .flatMap { authResponse =>
        val expiry    = OffsetDateTime.now().plus(authResponse.expires_in.toLong, ChronoUnit.SECONDS)
        val authToken = TadoAuthToken(authResponse.access_token, expiry)
        setAuthToken(authToken)
      }
      .handleErrorWith { error =>
        logger.debug("Refresh token failed with error", error) >>
        logger.warn("Unauthenticated, trying to login...") >>
        loginRequest()
      }
      .onError { error =>
        logger.error("Error while logging in", error)
      }

  private def useAuthClient[A](f: Client[F] => F[A]): F[A] =
    def retrieve(retry: Boolean): F[Client[F]] =
      getAuthToken().flatMap:
        case None if !retry =>
          Async[F].raiseError(Tado4sError("Tado4s could not log in"))
        case None =>
          Async[F].raiseError(Tado4sError("Tado4s is not logged in"))
        case Some(authToken) if isTokenExpired(authToken) =>
          refreshTokenRequest() >> retrieve(false)
        case Some(authToken) =>
          getAuthenticatedClient().flatMap:
            case None =>
              val client = buildAuthenticatedClient(authToken)
              setAuthenticatedClient(client) >> retrieve(false)
            case Some(client) =>
              Async[F].pure(client)

    retrieve(true).flatMap(f)

  private def buildAuthenticatedClient(authToken: TadoAuthToken): Client[F] =
    Client { request =>
      val authorization = Authorization(Credentials.Token(AuthScheme.Bearer, authToken.bearerToken))
      val authHeaders   = request.headers.put(authorization)
      val authRequest   = request.withHeaders(authHeaders)
      httpClient.run(authRequest)
    }

  private def isTokenExpired(authToken: TadoAuthToken): Boolean =
    authToken.expiry.minus(5, ChronoUnit.SECONDS) isBefore OffsetDateTime.now()

  //  State management  //

  private def getCredentials(): F[TadoCredentials] =
    atomicState.get.flatMap: state =>
      state.credentials match
        case Some(credentials) => Async[F].pure(credentials)
        case None              => Async[F].raiseError(Tado4sError("No credentials have been provided"))

  private def getAuthenticatedClient(): F[Option[Client[F]]] =
    atomicState.get.map:
      _.useAuthClient

  private def setAuthenticatedClient(client: Client[F]): F[Unit] =
    atomicState.update:
      _.copy(useAuthClient = Some(client))

  private def clearAuthenticatedClient(): F[Unit] =
    atomicState.update:
      _.copy(useAuthClient = None)

  private def setAuthToken(authToken: TadoAuthToken): F[Unit] =
    atomicState.update:
      _.copy(authToken = Some(authToken))

  private def getAuthToken(): F[Option[TadoAuthToken]] =
    atomicState.get.map:
      _.authToken

  private def clearAuthToken(): F[Unit] =
    atomicState.update:
      _.copy(authToken = None)

  private def setCredentials(username: String, password: String): F[Unit] =
    atomicState.update:
      _.copy(credentials = Some(TadoCredentials(username = username, password = password)))

  private def clearCredentials(): F[Unit] =
    atomicState.update:
      _.copy(credentials = None)

/**
 * Tado Client for Scala
 */
object Tado4sClient:

  final case class TadoClientState[F[_]](
    credentials: Option[TadoCredentials] = None,
    authToken: Option[TadoAuthToken] = None,
    useAuthClient: Option[Client[F]] = None,
  )

  final case class TadoCredentials(
    username: String,
    password: String,
  )

  final case class TadoAuthToken(
    bearerToken: String,
    expiry: OffsetDateTime,
  )

  /**
   * Creates a new instance of Tado4s client using the given client
   */
  def apply[F[_]: Async](httpClient: Client[F]): F[Tado4sClient[F]] =
    for
      initialState <- AtomicCell[F].of(TadoClientState[F](None, None, None))
      client        = new Tado4sClient[F](httpClient, TadoConfig.config, initialState)
    yield client

  /**
   * Creates a new instance of Tado4s client using http4s Ember Client
   */
  def clientF[F[_]: Async: Network](): F[Tado4sClient[F]] =
    EmberClientBuilder
      .default[F]
      .build
      .allocated
      .flatMap {
        case (httpClient, _) => Tado4sClient(httpClient)
      }
