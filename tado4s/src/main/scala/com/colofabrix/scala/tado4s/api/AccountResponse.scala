package com.colofabrix.scala.tado4s.api

import io.circe.Decoder
import java.util.UUID

/**
 * AccountResponse
 */
final case class AccountResponse(
  name: String,
  email: String,
  username: String,
  id: UUID,
  homes: Vector[AccountResponse.Home],
  locale: String,
  mobileDevices: Vector[AccountResponse.MobileDevice],
) derives Decoder

object AccountResponse {

  final case class Home(
    id: Int,
    name: String,
  ) derives Decoder

  final case class MobileDevice(
    name: String,
    id: Int,
    settings: Settings,
    location: Location,
    deviceMetadata: DeviceMetadata,
  ) derives Decoder

  final case class DeviceMetadata(
    platform: String,
    osVersion: String,
    model: String,
    locale: String, // Can be changed to java.util.Locale
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

  final case class Settings(
    geoTrackingEnabled: Boolean,
    specialOffersEnabled: Boolean,
    onDemandLogRetrievalEnabled: Boolean,
    pushNotifications: Option[PushNotifications],
  ) derives Decoder

  final case class PushNotifications(
    lowBatteryReminder: Boolean,
    awayModeReminder: Boolean,
    homeModeReminder: Boolean,
    openWindowReminder: Boolean,
    energySavingsReportReminder: Boolean,
    incidentDetection: Boolean,
    energyIqReminder: Boolean,
    tariffHighPriceAlert: Boolean,
    tariffLowPriceAlert: Boolean
  ) derives Decoder

}
