package com.colofabrix.scala.tado4s

import cats.effect.Async
import cats.implicits.*
import com.colofabrix.scala.http4s.middleware.betterlogger.ClientLogger
import com.colofabrix.scala.tado4s.api.*
import com.colofabrix.scala.tado4s.security.*
import com.colofabrix.scala.tado4s.store.TadoRefreshToken
import fs2.io.net.Network
import fs2.io.net.tls.TLSContext
import java.time.LocalDate
import org.http4s.*
import org.http4s.circe.CirceEntityDecoder.*
import org.http4s.circe.CirceEntityEncoder.*
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.Method.*
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

/**
 * Tado Client for Scala
 *
 * Reference: https://blog.scphillips.com/posts/2017/01/the-tado-api-v2/
 *            https://kritsel.github.io/tado-openapispec-v2/swagger.html
 */
final class Tado4sClient[F[_]: Async] private (
  config: TadoConfig,
  authenticator: Tado4sAuthentication[F],
) extends Http4sClientDsl[F] {

  implicit private val logger: SelfAwareStructuredLogger[F] =
    Slf4jLogger.getLogger[F]

  /**
   * Authenticate with a refresh token.
   */
  def authenticate(initialRefreshToken: TadoRefreshToken): F[Unit] =
    logger.debug("Authenticating with refresh token") >>
    authenticator.authenticate(initialRefreshToken)

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
      client <- authenticator.getAuthenticatedClient()
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
      client <- authenticator.getAuthenticatedClient()
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
      client <- authenticator.getAuthenticatedClient()
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
      client <- authenticator.getAuthenticatedClient()
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
      client <- authenticator.getAuthenticatedClient()
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
      client <- authenticator.getAuthenticatedClient()
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
      client <- authenticator.getAuthenticatedClient()
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
      client <- authenticator.getAuthenticatedClient()
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
      client <- authenticator.getAuthenticatedClient()
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
      client  <- authenticator.getAuthenticatedClient()
      url      = config.apiBase / "homes" / homeId / "zones" / zoneId / "dayReport"
      queryUrl = url.withQueryParam("date", date.toString())
      result  <- client.expectOr[DayReportResponse](GET(queryUrl))(handleClientExpectError)
      _       <- logger.trace(s"Response for getZoneDayReport(): $result")
    yield result

  //  Zone Control APIs  //

  /**
   * Zone capabilities
   */
  def getZoneCapabilities(request: ZoneCapabilitiesRequest): F[ZoneCapabilitiesResponse] =
    for
      _      <- logger.debug(s"Called getZoneCapabilities(): homeId=${request.homeId}, zoneId=${request.zoneId}")
      client <- authenticator.getAuthenticatedClient()
      url     = config.apiBase / "homes" / request.homeId / "zones" / request.zoneId / "capabilities"
      result <- client.expectOr[ZoneCapabilitiesResponse](GET(url))(handleClientExpectError)
      _      <- logger.trace(s"Response for getZoneCapabilities(): $result")
    yield result

  /**
   * Get early start settings
   */
  def getEarlyStart(request: GetEarlyStartRequest): F[EarlyStartResponse] =
    for
      _      <- logger.debug(s"Called getEarlyStart(): homeId=${request.homeId}, zoneId=${request.zoneId}")
      client <- authenticator.getAuthenticatedClient()
      url     = config.apiBase / "homes" / request.homeId / "zones" / request.zoneId / "earlyStart"
      result <- client.expectOr[EarlyStartResponse](GET(url))(handleClientExpectError)
      _      <- logger.trace(s"Response for getEarlyStart(): $result")
    yield result

  /**
   * Set early start settings
   */
  def setEarlyStart(request: SetEarlyStartRequest): F[EarlyStartResponse] =
    for
      _      <- logger.debug(s"Called setEarlyStart(): homeId=${request.homeId}, zoneId=${request.zoneId}")
      client <- authenticator.getAuthenticatedClient()
      url     = config.apiBase / "homes" / request.homeId / "zones" / request.zoneId / "earlyStart"
      body    = EarlyStartResponse(enabled = request.enabled)
      result <- client.expectOr[EarlyStartResponse](PUT(body, url))(handleClientExpectError)
      _      <- logger.trace(s"Response for setEarlyStart(): $result")
    yield result

  /**
   * Get active timetable
   */
  def getActiveTimetable(request: GetActiveTimetableRequest): F[ActiveTimetableResponse] =
    for
      _      <- logger.debug(s"Called getActiveTimetable(): homeId=${request.homeId}, zoneId=${request.zoneId}")
      client <- authenticator.getAuthenticatedClient()
      url     = config.apiBase / "homes" / request.homeId / "zones" / request.zoneId / "schedule" / "activeTimetable"
      result <- client.expectOr[ActiveTimetableResponse](GET(url))(handleClientExpectError)
      _      <- logger.trace(s"Response for getActiveTimetable(): $result")
    yield result

  /**
   * Set active timetable
   */
  def setActiveTimetable(request: SetActiveTimetableRequest): F[ActiveTimetableResponse] =
    for
      _      <- logger.debug(s"Called setActiveTimetable(): homeId=${request.homeId}, zoneId=${request.zoneId}")
      client <- authenticator.getAuthenticatedClient()
      url     = config.apiBase / "homes" / request.homeId / "zones" / request.zoneId / "schedule" / "activeTimetable"
      body    = ActiveTimetableResponse(id = request.timetableId, `type` = "ONE_DAY")
      result <- client.expectOr[ActiveTimetableResponse](PUT(body, url))(handleClientExpectError)
      _      <- logger.trace(s"Response for setActiveTimetable(): $result")
    yield result

  /**
   * Get all timetables
   */
  def getTimetables(request: GetTimetablesRequest): F[Vector[TimetablesResponse.Timetable]] =
    for
      _      <- logger.debug(s"Called getTimetables(): homeId=${request.homeId}, zoneId=${request.zoneId}")
      client <- authenticator.getAuthenticatedClient()
      url     = config.apiBase / "homes" / request.homeId / "zones" / request.zoneId / "schedule" / "timetables"
      result <- client.expectOr[Vector[TimetablesResponse.Timetable]](GET(url))(handleClientExpectError)
      _      <- logger.trace(s"Response for getTimetables(): $result")
    yield result

  /**
   * Get timetable blocks
   */
  def getTimetableBlocks(request: GetTimetableBlocksRequest): F[Vector[TimetableBlocksResponse.Block]] =
    val homeId      = request.homeId
    val zoneId      = request.zoneId
    val timetableId = request.timetableId
    for
      _      <- logger.debug(s"Called getTimetableBlocks(): homeId=$homeId, zoneId=$zoneId, timetableId=$timetableId")
      client <- authenticator.getAuthenticatedClient()
      url     = config.apiBase / "homes" / homeId / "zones" / zoneId / "schedule" / "timetables" / timetableId / "blocks"
      result <- client.expectOr[Vector[TimetableBlocksResponse.Block]](GET(url))(handleClientExpectError)
      _      <- logger.trace(s"Response for getTimetableBlocks(): $result")
    yield result

  /**
   * Get away configuration
   */
  def getAwayConfiguration(request: GetAwayConfigurationRequest): F[AwayConfigurationResponse] =
    for
      _      <- logger.debug(s"Called getAwayConfiguration(): homeId=${request.homeId}, zoneId=${request.zoneId}")
      client <- authenticator.getAuthenticatedClient()
      url     = config.apiBase / "homes" / request.homeId / "zones" / request.zoneId / "schedule" / "awayConfiguration"
      result <- client.expectOr[AwayConfigurationResponse](GET(url))(handleClientExpectError)
      _      <- logger.trace(s"Response for getAwayConfiguration(): $result")
    yield result

  /**
   * Set away configuration
   */
  def setAwayConfiguration(request: SetAwayConfigurationRequest): F[AwayConfigurationResponse] =
    val body =
      AwayConfigurationResponse(
        `type` = request.`type`,
        autoAdjust = request.autoAdjust,
        comfortLevel = request.comfortLevel,
        setting = request.setting,
      )
    for
      _      <- logger.debug(s"Called setAwayConfiguration(): homeId=${request.homeId}, zoneId=${request.zoneId}")
      client <- authenticator.getAuthenticatedClient()
      url     = config.apiBase / "homes" / request.homeId / "zones" / request.zoneId / "schedule" / "awayConfiguration"
      result <- client.expectOr[AwayConfigurationResponse](PUT(body, url))(handleClientExpectError)
      _      <- logger.trace(s"Response for setAwayConfiguration(): $result")
    yield result

  /**
   * Set zone overlay (manual control)
   */
  def setZoneOverlay(request: SetZoneOverlayRequest): F[ZoneOverlayResponse] =
    for
      _      <- logger.debug(s"Called setZoneOverlay(): homeId=${request.homeId}, zoneId=${request.zoneId}")
      client <- authenticator.getAuthenticatedClient()
      url     = config.apiBase / "homes" / request.homeId / "zones" / request.zoneId / "overlay"
      body    = SetZoneOverlayBody(setting = request.setting, termination = request.termination)
      result <- client.expectOr[ZoneOverlayResponse](PUT(body, url))(handleClientExpectError)
      _      <- logger.trace(s"Response for setZoneOverlay(): $result")
    yield result

  /**
   * Delete zone overlay
   */
  def deleteZoneOverlay(request: DeleteZoneOverlayRequest): F[Unit] =
    for
      _      <- logger.debug(s"Called deleteZoneOverlay(): homeId=${request.homeId}, zoneId=${request.zoneId}")
      client <- authenticator.getAuthenticatedClient()
      url     = config.apiBase / "homes" / request.homeId / "zones" / request.zoneId / "overlay"
      _      <- client.successful(DELETE(url))
      _      <- logger.trace(s"Response for deleteZoneOverlay(): success")
    yield ()

  //  Home Control APIs  //

  /**
   * Set home presence
   */
  def setHomePresence(request: SetHomePresenceRequest): F[Unit] =
    for
      _      <- logger.debug(s"Called setHomePresence(): homeId=${request.homeId}")
      client <- authenticator.getAuthenticatedClient()
      url     = config.apiBase / "homes" / request.homeId / "presence"
      body    = SetHomePresenceBody(homePresence = request.homePresence)
      _      <- client.successful(PUT(body, url))
      _      <- logger.trace(s"Response for setHomePresence(): success")
    yield ()

  /**
   * Get mobile devices
   */
  def getMobileDevices(request: GetMobileDevicesRequest): F[Vector[MobileDevicesResponse.MobileDevice]] =
    for
      _      <- logger.debug(s"Called getMobileDevices(): homeId=${request.homeId}")
      client <- authenticator.getAuthenticatedClient()
      url     = config.apiBase / "homes" / request.homeId / "mobileDevices"
      result <- client.expectOr[Vector[MobileDevicesResponse.MobileDevice]](GET(url))(handleClientExpectError)
      _      <- logger.trace(s"Response for getMobileDevices(): $result")
    yield result

  /**
   * Get mobile device settings
   */
  def getMobileDeviceSettings(request: GetMobileDeviceSettingsRequest): F[MobileDeviceSettingsResponse] =
    val homeId   = request.homeId
    val deviceId = request.mobileDeviceId
    for
      _      <- logger.debug(s"Called getMobileDeviceSettings(): homeId=$homeId, mobileDeviceId=$deviceId")
      client <- authenticator.getAuthenticatedClient()
      url     = config.apiBase / "homes" / homeId / "mobileDevices" / deviceId / "settings"
      result <- client.expectOr[MobileDeviceSettingsResponse](GET(url))(handleClientExpectError)
      _      <- logger.trace(s"Response for getMobileDeviceSettings(): $result")
    yield result

  /**
   * Set mobile device settings
   */
  def setMobileDeviceSettings(request: SetMobileDeviceSettingsRequest): F[MobileDeviceSettingsResponse] =
    val homeId   = request.homeId
    val deviceId = request.mobileDeviceId
    val body     = MobileDeviceSettingsResponse(request.geoTrackingEnabled, request.pushNotifications)
    for
      _      <- logger.debug(s"Called setMobileDeviceSettings(): homeId=$homeId, mobileDeviceId=$deviceId")
      client <- authenticator.getAuthenticatedClient()
      url     = config.apiBase / "homes" / homeId / "mobileDevices" / deviceId / "settings"
      result <- client.expectOr[MobileDeviceSettingsResponse](PUT(body, url))(handleClientExpectError)
      _      <- logger.trace(s"Response for setMobileDeviceSettings(): $result")
    yield result

  /**
   * Delete mobile device
   */
  def deleteMobileDevice(request: DeleteMobileDeviceRequest): F[Unit] =
    val homeId   = request.homeId
    val deviceId = request.mobileDeviceId
    for
      _      <- logger.debug(s"Called deleteMobileDevice(): homeId=$homeId, mobileDeviceId=$deviceId")
      client <- authenticator.getAuthenticatedClient()
      url     = config.apiBase / "homes" / homeId / "mobileDevices" / deviceId
      _      <- client.successful(DELETE(url))
      _      <- logger.trace(s"Response for deleteMobileDevice(): success")
    yield ()

  /**
   * Get heating circuits
   */
  def getHeatingCircuits(request: GetHeatingCircuitsRequest): F[Vector[HeatingCircuitsResponse.HeatingCircuit]] =
    for
      _      <- logger.debug(s"Called getHeatingCircuits(): homeId=${request.homeId}")
      client <- authenticator.getAuthenticatedClient()
      url     = config.apiBase / "homes" / request.homeId / "heatingCircuits"
      result <- client.expectOr[Vector[HeatingCircuitsResponse.HeatingCircuit]](GET(url))(handleClientExpectError)
      _      <- logger.trace(s"Response for getHeatingCircuits(): $result")
    yield result

  /**
   * Get air comfort
   */
  def getAirComfort(request: GetAirComfortRequest): F[AirComfortResponse] =
    for
      _      <- logger.debug(s"Called getAirComfort(): homeId=${request.homeId}")
      client <- authenticator.getAuthenticatedClient()
      url     = config.apiBase / "homes" / request.homeId / "airComfort"
      result <- client.expectOr[AirComfortResponse](GET(url))(handleClientExpectError)
      _      <- logger.trace(s"Response for getAirComfort(): $result")
    yield result

  //  Helper case classes for request bodies  //

  final private case class SetZoneOverlayBody(
    setting: ZoneOverlayRequest.Setting,
    termination: ZoneOverlayRequest.Termination,
  ) derives io.circe.Encoder.AsObject

  final private case class SetHomePresenceBody(
    homePresence: String,
  ) derives io.circe.Encoder.AsObject

  //  Error handlers  //

  private def handleClientExpectError(response: Response[F]): F[Throwable] =
    response
      .as[TadoErrorResponse]
      .map { error =>
        Tado4sError("Tado Request Error", Some(error))
      }

}

/**
 * Tado Client for Scala
 */
object Tado4sClient {

  /**
   * Creates a new instance of Tado4s client using http4s Ember Client
   */
  def apply[F[_]: Async: Network](maybeConfig: Option[TadoConfig]): F[Tado4sClient[F]] =
    for
      config        <- maybeConfig.getOrElse(TadoConfig.config).pure[F]
      tlsContext    <- buildTlsContext(config)
      httpClient    <- buildHttpClient(config, tlsContext)
      authenticator <- Tado4sAuthentication(httpClient, config)
      client         = new Tado4sClient[F](config, authenticator)
    yield client

  private def buildHttpClient[F[_]: Async: Network](config: TadoConfig, tlsContext: TLSContext[F]): F[Client[F]] =
    EmberClientBuilder
      .default[F]
      .withTimeout(config.httpTimeout)
      .withTLSContext(tlsContext)
      .build
      .allocated
      .map {
        case (httpClient, _) => ClientLogger(SSLValidationClient(config.ignoreSsl)(httpClient))
      }

  private def buildTlsContext[F[_]: Network](config: TadoConfig): F[TLSContext[F]] =
    if config.ignoreSsl then Network[F].tlsContext.insecure
    else Network[F].tlsContext.system

}
