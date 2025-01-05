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
import org.http4s.client.middleware.{ Retry, RetryPolicy }
import org.http4s.client.middleware.Logger
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.Method.*
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import scala.concurrent.duration.*

/**
 * Tado Client for Scala
 *
 * Reference: https://blog.scphillips.com/posts/2017/01/the-tado-api-v2/
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
      _      <- logger.debug(s"Called getAccountInfo()")
      client <- withAuthClient()
      request = GET(config.apiBase / "me")
      result <- client.expectOr[AccountResponse](request)(handleClientExpectError)
      _      <- logger.trace(s"Response for getAccountInfo: $result")
    yield result

  /**
   * Information about a specific Home
   */
  def getHomeDetails(homeId: Int): F[HomeResponse] =
    for
      _      <- logger.debug(s"Called getHomeDetails(): homeId=$homeId")
      client <- withAuthClient()
      request = GET(config.apiBase / "homes" / homeId)
      result <- client.expectOr[HomeResponse](request)(handleClientExpectError)
      _      <- logger.trace(s"Response for getHomeDetails response: $result")
    yield result

  /**
   * Information about the zones of a specific Home
   */
  def getHomeZones(homeId: Int): F[Vector[HomeZonesResponse]] =
    for
      _      <- logger.debug(s"Called getHomeZones(): homeId=$homeId")
      client <- withAuthClient()
      request = GET(config.apiBase / "homes" / homeId / "zones")
      result <- client.expectOr[Vector[HomeZonesResponse]](request)(handleClientExpectError)
      _      <- logger.trace(s"Response for getHomeZones(): $result")
    yield result

  /**
   * Information about the state of a Home
   */
  def getHomeState(homeId: Int): F[HomeStateResponse] =
    for
      _      <- logger.debug(s"Called getHomeState(): homeId=$homeId")
      client <- withAuthClient()
      request = GET(config.apiBase / "homes" / homeId / "state")
      result <- client.expectOr[HomeStateResponse](request)(handleClientExpectError)
      _      <- logger.trace(s"Response for getHomeState(): $result")
    yield result

  /**
   * State of a specific Zone in a specific Home
   */
  def getZoneState(homeId: Int, zoneId: Int): F[ZoneStateResponse] =
    for
      _      <- logger.debug(s"Called getZoneState(): homeId=$homeId, zoneId=$zoneId")
      client <- withAuthClient()
      request = GET(config.apiBase / "homes" / homeId / "zones" / zoneId / "state")
      result <- client.expectOr[ZoneStateResponse](request)(handleClientExpectError)
      _      <- logger.trace(s"Response for getZoneState(): $result")
    yield result

  /**
   * The weather reported at the house
   */
  def getHomeWeather(homeId: Int): F[WeatherResponse] =
    for
      _      <- logger.debug(s"Called getHomeWeather(): homeId=$homeId")
      client <- withAuthClient()
      request = GET(config.apiBase / "homes" / homeId / "weather")
      result <- client.expectOr[WeatherResponse](request)(handleClientExpectError)
      _      <- logger.trace(s"Response for getHomeWeather response: $result")
    yield result

  /**
   * The a daily reportfor a specific house and zone
   */
  def getZoneDayReport(homeId: Int, zoneId: Int, date: LocalDate): F[DayReportResponse] =
    for
      _       <- logger.debug(s"Called getZoneDayReport(): homeId=$homeId, zoneId=$zoneId, date=$date")
      client  <- withAuthClient()
      url      = config.apiBase / "homes" / homeId / "zones" / zoneId / "dayReport"
      queryUrl = url.withQueryParam("date", date.toString())
      result  <- client.expectOr[DayReportResponse](GET(queryUrl))(handleClientExpectError)
      _       <- logger.trace(s"Response for getZoneDayReport response: $result")
    yield result

  //  Internal operations  //

  private def loginRequest(): F[Unit] =
    getCredentials().flatMap: credentials =>
      val requestBody =
        UrlForm(
          "client_id"     -> "tado-web-app",
          "client_secret" -> config.clientSecret,
          "grant_type"    -> "password",
          "password"      -> credentials.password,
          "scope"         -> "home.user",
          "username"      -> credentials.username,
        )

      val postRequest = POST(requestBody, config.apiAuth / "oauth" / "token")

      logger.info("Sending login request") >>
      atomicallyModifyAuthToken: _ =>
        httpClient
          .expect[AuthResponse](postRequest)
          .flatMap: authResponse =>
            val expiry    = OffsetDateTime.now().plus(authResponse.expires_in.toLong, ChronoUnit.SECONDS)
            val authToken = TadoAuthToken(authResponse.access_token, authResponse.refresh_token, expiry)
            logger.trace(s"Login authentication response: $authResponse}") >>
            logger.debug(s"Logged in") >>
            Async[F].pure(Some(authToken))
      .adaptError: error =>
        Tado4sError("Error while logging in", Some(error))

  private def refreshTokenRequest(): F[Unit] =
    // See https://www.oauth.com/oauth2-servers/making-authenticated-requests/refreshing-an-access-token/
    getAuthToken().flatMap:
      case None =>
        logger.warn("Cannot refresh token because unauthenticated, trying to login...") >>
        loginRequest()
      case Some(authToken) =>
        val requestBody =
          UrlForm(
            "client_id"     -> "tado-web-app",
            "client_secret" -> config.clientSecret,
            "grant_type"    -> "refresh_token",
            "refresh_token" -> authToken.refreshToken,
            "scope"         -> "home.user",
          )

        val postRequest = POST(requestBody, config.apiAuth / "oauth" / "token")

        logger.debug("Sending refresh token request") >>
        atomicallyModifyAuthToken: _ =>
          httpClient
            .expect[AuthResponse](postRequest)
            .flatMap: authResponse =>
              val expiry       = OffsetDateTime.now().plus(authResponse.expires_in.toLong, ChronoUnit.SECONDS)
              val newAuthToken = TadoAuthToken(authResponse.access_token, authResponse.refresh_token, expiry)
              logger.trace(s"Refresh authentication response: $authResponse}") >>
              logger.info("Auth token refreshed") >>
              Async[F].pure(Some(newAuthToken))
        .handleErrorWith: error =>
          logger.debug(error)("Refresh token failed with error") >>
          logger.warn("Cannot refresh token, trying to login...") >>
          clearAuthToken() >>
          loginRequest()
        .onError: error =>
          logger.error(error)("Error while logging in")
        .adaptError: error =>
          Tado4sError("Error while refreshing API token", Some(error))

  private def withAuthClient[A](retries: Int = 1): F[Client[F]] =
    getAuthToken().flatMap:
      case None if retries <= 0 =>
        Async[F].raiseError(Tado4sError("Tado4s could not log in"))
      case None =>
        Async[F].raiseError(Tado4sError("Tado4s is not logged in"))
      case Some(authToken) if isTokenExpired(authToken) =>
        logger.info("Tado token expired, getting a new one") >>
        logger.trace(s"Expired token: $authToken") >>
        clearAuthenticatedClient() >>
        refreshTokenRequest() >>
        withAuthClient(retries - 1)
      case Some(authToken) =>
        getAuthenticatedClient().flatMap:
          case Some(client) =>
            logger.trace(s"Returning Tado authenticated client with token $authToken") >>
            Async[F].pure(client)
          case None =>
            for
              _      <- setAuthenticatedClient(buildHttpClient(authToken))
              _      <- logger.debug("Creating new Tado authenticated client")
              result <- withAuthClient(retries - 1)
            yield result

  private def buildHttpClient(authToken: TadoAuthToken): Client[F] =
    val retryPolicy =
      RetryPolicy[F](
        backoff = RetryPolicy.exponentialBackoff(config.maxRetryTime, config.maxRetries),
        retriable = (_, _) => true,
      )

    Retry(retryPolicy):
      TadoAuthenticatedClient[F](authToken.bearerToken):
        httpClient

  private def isTokenExpired(authToken: TadoAuthToken): Boolean =
    val now = OffsetDateTime.now()
    authToken.expiry.isBefore(now) || authToken.expiry.isEqual(now)

  //  Error handlers  //

  private def handleClientExpectError(response: Response[F]): F[Throwable] =
    response
      .as[TadoErrorResponse]
      .map { body =>
        Tado4sRequestError("Tado Request Error", body)
      }

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

  private def atomicallyModifyAuthToken(f: Option[TadoAuthToken] => F[Option[TadoAuthToken]]): F[Unit] =
    atomicState.evalModify: state =>
      f(state.authToken).map: newAuthToken =>
        (state.copy(authToken = newAuthToken), ())

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
    refreshToken: String,
    expiry: OffsetDateTime,
  ) {
    override def toString(): String =
      s"TadoAuthToken(" +
      s"bearerToken=${bearerToken.take(8)}...${bearerToken.takeRight(8)}, " +
      s"refreshToken=${refreshToken.take(8)}...${refreshToken.takeRight(8)}, " +
      s"expiry=${expiry.truncatedTo(ChronoUnit.SECONDS).toLocalDateTime}" +
      s")"
  }

  /** Creates a new instance of Tado4s client using http4s Ember Client */
  def apply[F[_]: Async: Network](maybeConfig: Option[TadoConfig]): F[Tado4sClient[F]] =
    EmberClientBuilder
      .default[F]
      .withTimeout(30.seconds)
      .build
      .allocated
      .flatMap {
        case (httpClient, _) =>
          val config = maybeConfig.getOrElse(TadoConfig.config)
          Tado4sClient(config, httpClient)
      }

  private def apply[F[_]: Async](config: TadoConfig, httpClient: Client[F]): F[Tado4sClient[F]] =
    for
      initialState    <- AtomicCell[F].of(TadoClientState[F](None, None, None))
      loggedHttpClient = Logger.colored[F](logBody = true, logHeaders = true)(httpClient)
      client           = new Tado4sClient[F](loggedHttpClient, config, initialState)
    yield client
