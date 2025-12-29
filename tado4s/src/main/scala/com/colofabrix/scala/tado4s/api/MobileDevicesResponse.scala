package com.colofabrix.scala.tado4s.api

import io.circe.Decoder
import io.circe.Encoder

/** Response for mobile devices */
final case class MobileDevicesResponse(
  devices: Vector[MobileDevicesResponse.MobileDevice],
) derives Decoder

object MobileDevicesResponse {

  final case class MobileDevice(
    id: Int,
    name: String,
    settings: Settings,
    location: Option[Location],
    deviceMetadata: DeviceMetadata,
  ) derives Decoder

  final case class Settings(
    geoTrackingEnabled: Boolean,
    pushNotifications: Option[PushNotifications],
  ) derives Decoder

  final case class PushNotifications(
    lowBatteryReminder: Boolean,
    awayModeReminder: Boolean,
    homeModeReminder: Boolean,
    openWindowReminder: Boolean,
    energySavingsReportReminder: Boolean,
    incidentDetection: Boolean,
  ) derives Decoder

  final case class Location(
    stale: Boolean,
    atHome: Boolean,
    bearingFromHome: BearingFromHome,
    relativeDistanceFromHomeFence: Double,
  ) derives Decoder

  final case class BearingFromHome(
    degrees: Double,
    radians: Double,
  ) derives Decoder

  final case class DeviceMetadata(
    platform: String,
    osVersion: String,
    model: String,
    locale: String,
  ) derives Decoder

}

/** Response for mobile device settings */
final case class MobileDeviceSettingsResponse(
  geoTrackingEnabled: Boolean,
  pushNotifications: Option[MobileDeviceSettingsResponse.PushNotifications],
) derives Decoder, Encoder.AsObject

object MobileDeviceSettingsResponse {

  final case class PushNotifications(
    lowBatteryReminder: Boolean,
    awayModeReminder: Boolean,
    homeModeReminder: Boolean,
    openWindowReminder: Boolean,
    energySavingsReportReminder: Boolean,
    incidentDetection: Boolean,
  ) derives Decoder, Encoder.AsObject

}
