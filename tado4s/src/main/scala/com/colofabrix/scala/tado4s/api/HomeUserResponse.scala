package com.colofabrix.scala.tado4s.api

import io.circe.*

final case class HomeUserResponse (
  name: String,
  email: String,
  username: String,
  id: String,
  homes: Vector[Homes],
  locale: String,
  mobileDevices: Vector[MobileDevices]
) derives Decoder

final case class BearingFromHome (
  degrees: Double,
  radians: Double
) derives Decoder

final case class DeviceMetadata (
  platform: String,
  osVersion: String,
  model: String,
  locale: String
) derives Decoder

final case class Homes (
  id: Int,
  name: String
) derives Decoder

final case class Location (
  stale: Boolean,
  atHome: Boolean,
  bearingFromHome: BearingFromHome,
  relativeDistanceFromHomeFence: Double
) derives Decoder

final case class MobileDevices (
  name: String,
  id: Int,
  settings: Settings,
  location: Location,
  deviceMetadata: DeviceMetadata
) derives Decoder

final case class PushNotifications (
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

final case class Settings (
  geoTrackingEnabled: Boolean,
  specialOffersEnabled: Boolean,
  onDemandLogRetrievalEnabled: Boolean,
  pushNotifications: PushNotifications
) derives Decoder


