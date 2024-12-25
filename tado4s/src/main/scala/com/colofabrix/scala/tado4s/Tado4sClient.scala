package com.colofabrix.scala.tado4s

import cats.effect.Async
import cats.effect.std.AtomicCell
import cats.implicits.given
import com.colofabrix.scala.tado4s.api.*
import com.colofabrix.scala.tado4s.Tado4sClient.*
import fs2.io.net.Network
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import org.http4s.*
import org.http4s.circe.CirceEntityDecoder.*
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.Method.*
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import scala.concurrent.duration.*

/**
 * Tado Client for Scala
 */
final class Tado4sClient[F[_]: Async] private (
  httpClient: Client[F],
  config: TadoConfig,
  atomicState: AtomicCell[F, TadoClientState[F]],
) extends Http4sClientDsl[F]:

  implicit private val logger: SelfAwareStructuredLogger[F] =
    Slf4jLogger.getLogger[F]

  /**
   * Logs into the Tado service
   */
  def login(username: String, password: String): F[Unit] =
    logger.debug(s"Login") >>
    setCredentials(username, password) >>
    loginRequest()

  /**
   * Logs out the Tado service
   */
  def logout(): F[Unit] =
    logger.debug(s"Logout") >>
    clearCredentials() >>
    clearAuthToken() >>
    clearAuthenticatedClient()

  /**
   * Information about the Tado account
   */
  def getAccountInfo(): F[AccountResponse] =
    for
      _      <- logger.debug(s"Get Account Info")
      client <- withAuthClient()
      request = GET(config.apiBase / "me")
      result <- client.expect[AccountResponse](request)
    yield result

  /**
   * Information about a specific Home
   */
  def getHomeDetails(homeId: Int): F[HomeResponse] =
    for
      _      <- logger.debug(s"Get Home Details, homeId=$homeId")
      client <- withAuthClient()
      request = GET(config.apiBase / "homes" / homeId)
      result <- client.expect[HomeResponse](request)
    yield result

  /**
   * Information about the zones of a specific Home
   */
  def getHomeZones(homeId: Int): F[Vector[HomeZonesResponse]] =
    for
      _      <- logger.debug(s"Get Home Zone, homeId=$homeId")
      client <- withAuthClient()
      request = GET(config.apiBase / "homes" / homeId / "zones")
      result <- client.expect[Vector[HomeZonesResponse]](request)
    yield result

  /**
   * Information about the state of a Home
   */
  def getHomeState(homeId: Int): F[HomeStateResponse] =
    for
      _      <- logger.debug(s"Get Home State, homeId=$homeId")
      client <- withAuthClient()
      request = GET(config.apiBase / "homes" / homeId / "state")
      result <- client.expect[HomeStateResponse](request)
    yield result

  /**
   * State of a specific Zone in a specific Home
   */
  def getZoneState(homeId: Int, zoneId: Int): F[ZoneStateResponse] =
    for
      _      <- logger.debug(s"Get Zone State, homeId=$homeId, zoneId=$zoneId")
      client <- withAuthClient()
      request = GET(config.apiBase / "homes" / homeId / "zones" / zoneId / "state")
      result <- client.expect[ZoneStateResponse](request)
    yield result

  /**
   * The weather reported at the house
   */
  def getHomeWeather(homeId: Int): F[WeatherResponse] =
    for
      _      <- logger.debug(s"Get Home Weather, homeId=$homeId")
      client <- withAuthClient()
      request = GET(config.apiBase / "homes" / homeId / "weather")
      result <- client.expect[WeatherResponse](request)
    yield result

  /**
   * The a daily reportfor a specific house and zone
   */
  def getZoneDayReport(homeId: Int, zoneId: Int, date: LocalDate): F[DayReportResponse] =
    for
      _       <- logger.debug(s"Get Zone State, homeId=$homeId, zoneId=$zoneId, date=$date")
      client  <- withAuthClient()
      url      = config.apiBase / "homes" / homeId / "zones" / zoneId / "dayReport"
      queryUrl = url.withQueryParam("date", date.toString())
      result  <- client.expect[DayReportResponse](GET(queryUrl))
    yield result

  //  Internal operations  //

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
        .adaptError { error =>
          Tado4sError("Error while logging in", Some(error))
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
        logger.debug(error)("Refresh token failed with error") >>
        logger.warn("Unauthenticated, trying to login...") >>
        loginRequest()
      }
      .onError { error =>
        logger.error(error)("Error while logging in")
      }
      .adaptError { error =>
        Tado4sError("Error while refreshing API token", Some(error))
      }

  private def withAuthClient[A](retries: Int = 1): F[Client[F]] =
    logger.trace("Getting Authenticated Client") >>
    getAuthToken().flatMap:
      case None if retries <= 0 =>
        Async[F].raiseError(Tado4sError("Tado4s could not log in"))
      case None =>
        Async[F].raiseError(Tado4sError("Tado4s is not logged in"))
      case Some(authToken) if isTokenExpired(authToken) =>
        logger.info("Tado token expired, getting a new one") >>
        refreshTokenRequest() >> withAuthClient(retries - 1)
      case Some(authToken) =>
        getAuthenticatedClient().flatMap:
          case Some(client) =>
            Async[F].pure(client)
          case None =>
            for
              client <- buildHttpClient(authToken)
              _      <- setAuthenticatedClient(client)
              _      <- logger.debug("New Tado authenticated client")
              result <- withAuthClient(retries - 1)
            yield result

  private def buildHttpClient(authToken: TadoAuthToken): F[Client[F]] =
    val authClient = TadoAuthenticatedClient[F](httpClient, authToken.bearerToken)
    TadoLoggedClient[F](authClient, logger)

  private def isTokenExpired(authToken: TadoAuthToken): Boolean =
    authToken.expiry.minus(5, ChronoUnit.SECONDS).isBefore(OffsetDateTime.now())

  //  State management  //

  private def getCredentials(): F[TadoCredentials] =
    atomicState.get.flatMap: state =>
      state.credentials match
        case Some(credentials) => Async[F].pure(credentials)
        case None              => Async[F].raiseError(Tado4sError("No Tado credentials provided"))

  private def getAuthenticatedClient(): F[Option[Client[F]]] =
    atomicState.get.map:
      _.authenticatedClient

  private def setAuthenticatedClient(client: Client[F]): F[Unit] =
    atomicState.update:
      _.copy(authenticatedClient = Some(client))

  private def clearAuthenticatedClient(): F[Unit] =
    atomicState.update:
      _.copy(authenticatedClient = None)

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

  final private case class TadoClientState[F[_]](
    credentials: Option[TadoCredentials] = None,
    authToken: Option[TadoAuthToken] = None,
    authenticatedClient: Option[Client[F]] = None,
  )

  final private case class TadoCredentials(
    username: String,
    password: String,
  )

  final private case class TadoAuthToken(
    bearerToken: String,
    expiry: OffsetDateTime,
  )

  /** Creates a new instance of Tado4s client using the given client */
  def apply[F[_]: Async](maybeConfig: Option[TadoConfig], httpClient: Client[F]): F[Tado4sClient[F]] =
    for
      initialState <- AtomicCell[F].of(TadoClientState[F](None, None, None))
      config        = maybeConfig.getOrElse(TadoConfig.config)
      client        = new Tado4sClient[F](httpClient, config, initialState)
    yield client

  /** Creates a new instance of Tado4s client using http4s Ember Client */
  def apply[F[_]: Async: Network](maybeConfig: Option[TadoConfig]): F[Tado4sClient[F]] =
    EmberClientBuilder
      .default[F]
      .withTimeout(30.seconds)
      .build
      .allocated
      .flatMap {
        case (httpClient, _) => Tado4sClient(maybeConfig, httpClient)
      }
