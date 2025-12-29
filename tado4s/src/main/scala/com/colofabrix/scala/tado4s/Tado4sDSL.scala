package com.colofabrix.scala.tado4s

import com.colofabrix.scala.tado4s.api.*
import java.time.LocalDate

/**
 * User-friendly DSL for Tado4s client with default parameters
 */
trait Tado4sDSL {

  extension [F[_]](client: Tado4sClient[F])

    //  Zone Control DSL  //

    /**
     * Get zone capabilities with simplified signature
     */
    def zoneCapabilities(homeId: Int, zoneId: Int): F[ZoneCapabilitiesResponse] =
      client.getZoneCapabilities(ZoneCapabilitiesRequest(homeId = homeId, zoneId = zoneId))

    /**
     * Get early start settings with simplified signature
     */
    def earlyStart(homeId: Int, zoneId: Int): F[EarlyStartResponse] =
      client.getEarlyStart(GetEarlyStartRequest(homeId = homeId, zoneId = zoneId))

    /**
     * Enable or disable early start
     */
    def setEarlyStartEnabled(homeId: Int, zoneId: Int, enabled: Boolean): F[EarlyStartResponse] =
      client.setEarlyStart(SetEarlyStartRequest(homeId = homeId, zoneId = zoneId, enabled = enabled))

    /**
     * Get active timetable with simplified signature
     */
    def activeTimetable(homeId: Int, zoneId: Int): F[ActiveTimetableResponse] =
      client.getActiveTimetable(GetActiveTimetableRequest(homeId = homeId, zoneId = zoneId))

    /**
     * Set active timetable
     */
    def activateTimetable(homeId: Int, zoneId: Int, timetableId: Int): F[ActiveTimetableResponse] =
      client.setActiveTimetable(SetActiveTimetableRequest(homeId = homeId, zoneId = zoneId, timetableId = timetableId))

    /**
     * Get all timetables with simplified signature
     */
    def timetables(homeId: Int, zoneId: Int): F[Vector[TimetablesResponse.Timetable]] =
      client.getTimetables(GetTimetablesRequest(homeId = homeId, zoneId = zoneId))

    /**
     * Get timetable blocks with simplified signature
     */
    def timetableBlocks(homeId: Int, zoneId: Int, timetableId: Int): F[Vector[TimetableBlocksResponse.Block]] =
      client.getTimetableBlocks(GetTimetableBlocksRequest(homeId = homeId, zoneId = zoneId, timetableId = timetableId))

    /**
     * Get away configuration with simplified signature
     */
    def awayConfiguration(homeId: Int, zoneId: Int): F[AwayConfigurationResponse] =
      client.getAwayConfiguration(GetAwayConfigurationRequest(homeId = homeId, zoneId = zoneId))

    //  Zone Overlay DSL  //

    /**
     * Set manual temperature control (heating on) until next schedule change
     */
    def setTemperature(
      homeId: Int,
      zoneId: Int,
      temperatureCelsius: Double,
      terminationType: String = "TADO_MODE",
      durationInSeconds: Option[Int] = None,
    ): F[ZoneOverlayResponse] =
      val termination = ZoneOverlayRequest.Termination(`type` = terminationType, durationInSeconds = durationInSeconds)
      val setting =
        ZoneOverlayRequest.Setting(
          `type` = "HEATING",
          power = "ON",
          temperature = Some(ZoneOverlayRequest.Temperature(celsius = Some(temperatureCelsius), fahrenheit = None)),
        )

      client.setZoneOverlay(SetZoneOverlayRequest(
        homeId = homeId,
        zoneId = zoneId,
        setting = setting,
        termination = termination,
      ))

    /**
     * Turn off heating until next schedule change
     */
    def turnOffHeating(
      homeId: Int,
      zoneId: Int,
      terminationType: String = "TADO_MODE",
      durationInSeconds: Option[Int] = None,
    ): F[ZoneOverlayResponse] =
      val setting     = ZoneOverlayRequest.Setting(`type` = "HEATING", power = "OFF", temperature = None)
      val termination = ZoneOverlayRequest.Termination(`type` = terminationType, durationInSeconds = durationInSeconds)

      client.setZoneOverlay(SetZoneOverlayRequest(
        homeId = homeId,
        zoneId = zoneId,
        setting = setting,
        termination = termination,
      ))

    /**
     * Resume schedule (delete overlay)
     */
    def resumeSchedule(homeId: Int, zoneId: Int): F[Unit] =
      client.deleteZoneOverlay(DeleteZoneOverlayRequest(homeId = homeId, zoneId = zoneId))

    //  Home Control DSL  //

    /**
     * Set home to HOME mode
     */
    def setHome(homeId: Int): F[Unit] =
      client.setHomePresence(SetHomePresenceRequest(homeId = homeId, homePresence = "HOME"))

    /**
     * Set home to AWAY mode
     */
    def setAway(homeId: Int): F[Unit] =
      client.setHomePresence(SetHomePresenceRequest(homeId = homeId, homePresence = "AWAY"))

    /**
     * Get mobile devices with simplified signature
     */
    def mobileDevices(homeId: Int): F[Vector[MobileDevicesResponse.MobileDevice]] =
      client.getMobileDevices(GetMobileDevicesRequest(homeId = homeId))

    /**
     * Get mobile device settings with simplified signature
     */
    def mobileDeviceSettings(homeId: Int, mobileDeviceId: Int): F[MobileDeviceSettingsResponse] =
      client.getMobileDeviceSettings(GetMobileDeviceSettingsRequest(homeId = homeId, mobileDeviceId = mobileDeviceId))

    /**
     * Enable or disable geo-tracking for a mobile device
     */
    def setGeoTracking(
      homeId: Int,
      mobileDeviceId: Int,
      enabled: Boolean,
      pushNotifications: Option[MobileDeviceSettingsResponse.PushNotifications] = None,
    ): F[MobileDeviceSettingsResponse] =
      client.setMobileDeviceSettings(
        SetMobileDeviceSettingsRequest(
          homeId = homeId,
          mobileDeviceId = mobileDeviceId,
          geoTrackingEnabled = enabled,
          pushNotifications = pushNotifications,
        ),
      )

    /**
     * Remove a mobile device
     */
    def removeMobileDevice(homeId: Int, mobileDeviceId: Int): F[Unit] =
      client.deleteMobileDevice(DeleteMobileDeviceRequest(homeId = homeId, mobileDeviceId = mobileDeviceId))

    /**
     * Get heating circuits with simplified signature
     */
    def heatingCircuits(homeId: Int): F[Vector[HeatingCircuitsResponse.HeatingCircuit]] =
      client.getHeatingCircuits(GetHeatingCircuitsRequest(homeId = homeId))

    /**
     * Get air comfort with simplified signature
     */
    def airComfort(homeId: Int): F[AirComfortResponse] =
      client.getAirComfort(GetAirComfortRequest(homeId = homeId))

    //  Existing API DSL wrappers  //

    /**
     * Get zone day report with simplified signature
     */
    def zoneDayReport(homeId: Int, zoneId: Int, date: LocalDate = LocalDate.now()): F[DayReportResponse] =
      client.getZoneDayReport(homeId, zoneId, date)

}

object Tado4sDSL extends Tado4sDSL
