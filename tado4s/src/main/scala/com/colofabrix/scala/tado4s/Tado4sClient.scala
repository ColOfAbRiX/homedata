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

  private val authenticator: Tado4sAuthentication[F] =
    new Tado4sAuthentication(httpClient, config, atomicState)

  /**
   * Logs into the Tado service
   */
  def login(username: String, password: String): F[Unit] =
    logger.debug("Login") >>
    authenticator.login(username, password)

  /**
   * Logs out the Tado service
   */
  def logout(): F[Unit] =
    logger.debug("Logout") >>
    authenticator.logout()

  /**
   * Information about the Tado account
   */
  def getAccountInfo(): F[AccountResponse] =
    for
      _      <- logger.debug(s"Called getAccountInfo()")
      client <- authenticator.withAuthClient()
      request = GET(config.apiBase / "me")
      result <- client.expectOr[AccountResponse](request)(handleClientExpectError)
      _      <- logger.trace(s"Response for getAccountInfo(): $result")
    yield result

  /**
   * Information about a specific Home
   */
  def getHomeDetails(homeId: Int): F[HomeResponse] =
    for
      _      <- logger.debug(s"Called getHomeDetails(): homeId=$homeId")
      client <- authenticator.withAuthClient()
      request = GET(config.apiBase / "homes" / homeId)
      result <- client.expectOr[HomeResponse](request)(handleClientExpectError)
      _      <- logger.trace(s"Response for getHomeDetails(): $result")
    yield result

  /**
   * Information about the zones of a specific Home
   */
  def getHomeZones(homeId: Int): F[Vector[HomeZoneResponse]] =
    for
      _      <- logger.debug(s"Called getHomeZones(): homeId=$homeId")
      client <- authenticator.withAuthClient()
      request = GET(config.apiBase / "homes" / homeId / "zones")
      result <- client.expectOr[Vector[HomeZoneResponse]](request)(handleClientExpectError)
      _      <- logger.trace(s"Response for getHomeZones(): $result")
    yield result

  /**
   * Information about the state of a Home
   */
  def getHomeState(homeId: Int): F[HomeStateResponse] =
    for
      _      <- logger.debug(s"Called getHomeState(): homeId=$homeId")
      client <- authenticator.withAuthClient()
      request = GET(config.apiBase / "homes" / homeId / "state")
      result <- client.expectOr[HomeStateResponse](request)(handleClientExpectError)
      _      <- logger.trace(s"Response for getHomeState(): $result")
    yield result

  /**
   * Information about all registered devices
   */
  def getHomeDevices(homeId: Int): F[Vector[HomeDeviceResponse]] =
    for
      _      <- logger.debug(s"Called getHomeDevices(): homeId=$homeId")
      client <- authenticator.withAuthClient()
      request = GET(config.apiBase / "homes" / homeId / "devices")
      result <- client.expectOr[Vector[HomeDeviceResponse]](request)(handleClientExpectError)
      _      <- logger.trace(s"Response for getHomeDevices(): $result")
    yield result

  /**
   * Information about a installations
   */
  def getHomeInstallations(homeId: Int): F[Vector[HomeInstallationResponse]] =
    for
      _      <- logger.debug(s"Called getHomeInstallations(): homeId=$homeId")
      client <- authenticator.withAuthClient()
      request = GET(config.apiBase / "homes" / homeId / "installations")
      result <- client.expectOr[Vector[HomeInstallationResponse]](request)(handleClientExpectError)
      _      <- logger.trace(s"Response for getHomeInstallations(): $result")
    yield result

  /**
   * Information about configured users
   */
  def getHomeUsers(homeId: Int): F[Vector[HomeUserResponse]] =
    for
      _      <- logger.debug(s"Called getHomeUsers(): homeId=$homeId")
      client <- authenticator.withAuthClient()
      request = GET(config.apiBase / "homes" / homeId / "users")
      result <- client.expectOr[Vector[HomeUserResponse]](request)(handleClientExpectError)
      _      <- logger.trace(s"Response for getHomeUsers(): $result")
    yield result

  /**
   * State of a specific Zone in a specific Home
   */
  def getZoneState(homeId: Int, zoneId: Int): F[ZoneStateResponse] =
    for
      _      <- logger.debug(s"Called getZoneState(): homeId=$homeId, zoneId=$zoneId")
      client <- authenticator.withAuthClient()
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
      client <- authenticator.withAuthClient()
      request = GET(config.apiBase / "homes" / homeId / "weather")
      result <- client.expectOr[WeatherResponse](request)(handleClientExpectError)
      _      <- logger.trace(s"Response for getHomeWeather(): $result")
    yield result

  /**
   * The a daily reportfor a specific house and zone
   */
  def getZoneDayReport(homeId: Int, zoneId: Int, date: LocalDate): F[DayReportResponse] =
    for
      _       <- logger.debug(s"Called getZoneDayReport(): homeId=$homeId, zoneId=$zoneId, date=$date")
      client  <- authenticator.withAuthClient()
      url      = config.apiBase / "homes" / homeId / "zones" / zoneId / "dayReport"
      queryUrl = url.withQueryParam("date", date.toString())
      result  <- client.expectOr[DayReportResponse](GET(queryUrl))(handleClientExpectError)
      _       <- logger.trace(s"Response for getZoneDayReport(): $result")
    yield result

  //  Error handlers  //

  private def handleClientExpectError(response: Response[F]): F[Throwable] =
    response
      .as[TadoErrorResponse]
      .map { error =>
        Tado4sError("Tado Request Error", Some(error))
      }

/**
 * Tado Client for Scala
 */
object Tado4sClient:

  final private[tado4s] case class TadoClientState[F[_]](
    credentials: Option[TadoCredentials] = None,
    authToken: Option[TadoAuthToken] = None,
    authenticatedClient: Option[Client[F]] = None,
  )

  final private[tado4s] case class TadoCredentials(
    username: String,
    password: String,
  )

  final private[tado4s] case class TadoAuthToken(
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
      .withTimeout(maybeConfig.map(_.httpTimeout).getOrElse(30.seconds))
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
