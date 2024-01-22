package com.colofabrix.scala.tado4s

import cats.effect.Async
import cats.effect.std.AtomicCell
import cats.implicits.given
import com.colofabrix.scala.tado4s.Tado4sClient.*
import fs2.io.net.Network
import io.odin.*
import io.odin.formatter.Formatter
import java.time.Instant
import java.time.LocalDate
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
    setCredentials(username, password) >> login()

  /**
   * Logs out the Tado service
   */
  def logout(): F[Unit] =
    clearCredentials() >> clearAuthToken()

  /**
   * Information about the Tado account
   */
  def getAccountInfo(): F[AccountResponse] =
    authenticated: client =>
      val request = GET(config.apiBase / "me")
      client.expect[AccountResponse](request)

  /**
   * Information about a specific Home
   */
  def getHomeDetails(homeId: Int): F[HomeResponse] =
    authenticated: client =>
      val request = GET(config.apiBase / "homes" / homeId)
      client.expect[HomeResponse](request)

  /**
   * Information about the zones of a specific Home
   */
  def getHomeZones(homeId: Int): F[Vector[HomeZonesResponse]] =
    authenticated: client =>
      val request = GET(config.apiBase / "homes" / homeId / "zones")
      client.expect[Vector[HomeZonesResponse]](request)

  /**
   * Information the state of a Home
   */
  def getHomeState(homeId: Int): F[HomeStateResponse] =
    authenticated: client =>
      val request = GET(config.apiBase / "homes" / homeId / "state")
      client.expect[HomeStateResponse](request)

  /**
   * State of a specific Zone in a specific Home
   */
  def getZoneState(homeId: Int, zoneId: Int): F[ZoneStateResponse] =
    authenticated: client =>
      val request = GET(config.apiBase / "homes" / homeId / "zones" / zoneId / "state")
      client.expect[ZoneStateResponse](request)

  /**
   * The weather reported at the house
   */
  def getHomeWeather(homeId: Int): F[WeatherResponse] =
    authenticated: client =>
      val request = GET(config.apiBase / "homes" / homeId / "weather")
      client.expect[WeatherResponse](request)

  /**
   * The weather reported at the house
   */
  def getDayReport(homeId: Int, zoneId: Int, date: LocalDate): F[DayReportResponse] =
    authenticated: client =>
      val url =
        (config.apiBase / "homes" / homeId / "zones" / zoneId / "dayReport")
          .withQueryParam("date", date.toString())

      client.expect[DayReportResponse](GET(url))

  private def login(): F[Unit] =
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
          val expiry    = Instant.now().plus(authResponse.expires_in.toLong, ChronoUnit.SECONDS)
          val authToken = TadoAuthToken(authResponse.access_token, expiry)
          setAuthToken(authToken)
        }
        .onError { error =>
          logger.error("Error while logging in", error)
        }

  private def refreshToken(): F[Unit] =
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
        val expiry    = Instant.now().plus(authResponse.expires_in.toLong, ChronoUnit.SECONDS)
        val authToken = TadoAuthToken(authResponse.access_token, expiry)
        setAuthToken(authToken)
      }
      .handleErrorWith { error =>
        logger.debug("Refresh token failed with error", error) >>
        logger.warn("Unauthenticated, trying to login...") >>
        login()
      }
      .onError { error =>
        logger.error("Error while logging in", error)
      }

  private def authenticated[A](f: Client[F] => F[A]): F[A] =
    getAuthToken().flatMap:
      case None =>
        Async[F].raiseError(Tado4sError("Tado4s is not logged in"))
      case Some(authToken) =>
        if isTokenExpired(authToken) then
          refreshToken() >> f(authenticatedClient(authToken))
        else
          f(authenticatedClient(authToken))

  private def authenticatedClient(authToken: TadoAuthToken): Client[F] =
    Client { request =>
      val authorization = Authorization(Credentials.Token(AuthScheme.Bearer, authToken.bearerToken))
      val authHeaders   = request.headers.put(authorization)
      val authRequest   = request.withHeaders(authHeaders)
      httpClient.run(authRequest)
    }

  private def isTokenExpired(authToken: TadoAuthToken): Boolean =
    authToken.expiry.minus(5, ChronoUnit.SECONDS) isBefore Instant.now()

  //  State management  //

  private def getCredentials(): F[TadoCredentials] =
    atomicState.get.flatMap: state =>
      state.credentials match
        case Some(credentials) => Async[F].pure(credentials)
        case None              => Async[F].raiseError(Tado4sError("No credentials have been provided"))

  private def setAuthToken(authToken: TadoAuthToken): F[Unit] =
    atomicState.update:
      _.copy(
        authToken = Some(authToken),
        authenticatedClient = Some(authenticatedClient(authToken)),
      )

  private def getAuthToken(): F[Option[TadoAuthToken]] =
    atomicState.get.map:
      _.authToken

  private def clearAuthToken(): F[Unit] =
    atomicState.update:
      _.copy(authToken = None, authenticatedClient = None)

  private def setCredentials(username: String, password: String): F[Unit] =
    atomicState.update:
      _.copy(credentials = Some(TadoCredentials(username = username, password = password)))

  private def clearCredentials(): F[Unit] =
    atomicState.update:
      _.copy(credentials = None)

end Tado4sClient

object Tado4sClient:

  final case class TadoClientState[F[_]](
    credentials: Option[TadoCredentials] = None,
    authToken: Option[TadoAuthToken] = None,
    authenticatedClient: Option[Client[F]] = None,
  )

  final case class TadoCredentials(
    username: String,
    password: String,
  )

  final case class TadoAuthToken(
    bearerToken: String,
    expiry: Instant,
  )

  def apply[F[_]: Async](httpClient: Client[F]): F[Tado4sClient[F]] =
    for
      initialState <- AtomicCell[F].of(TadoClientState[F](None, None, None))
      client        = new Tado4sClient[F](httpClient, TadoConfig.config, initialState)
    yield client

  def clientF[F[_]: Async: Network](): F[Tado4sClient[F]] =
    EmberClientBuilder
      .default[F]
      .build
      .allocated
      .flatMap {
        case (httpClient, _) => Tado4sClient(httpClient)
      }

end Tado4sClient
