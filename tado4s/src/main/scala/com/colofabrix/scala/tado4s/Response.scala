package com.colofabrix.scala.tado4s

import cats.effect.Async
import io.circe.Decoder
import org.http4s.circe.*
import org.http4s.EntityDecoder

sealed transparent trait TadoResponse

/**
 * AuthResponse
 */
final case class AuthResponse(
  access_token: String,
  token_type: String,
  refresh_token: String,
  expires_in: Int,
  scope: String,
  jti: String,
) extends TadoResponse derives Decoder

object AuthResponse:
  given [F[_]](using Async[F]): EntityDecoder[F, AuthResponse] = jsonOf[F, AuthResponse]

/**
 * AccountResponse
 */
final case class AccountResponse(
  name: String,
  email: String,
  username: String,
  id: String,
  homes: Vector[Home],
  locale: String,
  mobileDevices: Vector[MobileDevices],
) extends TadoResponse derives Decoder

object AccountResponse:
  given [F[_]](using Async[F]): EntityDecoder[F, AccountResponse] = jsonOf[F, AccountResponse]

/**
 * HomeResponse
 */
final case class HomeResponse(
  id: Int,
  name: String,
  dateTimeZone: String,
  dateCreated: String,
  temperatureUnit: String,
  partner: Option[String],
  simpleSmartScheduleEnabled: Boolean,
  awayRadiusInMeters: Double,
  installationCompleted: Boolean,
  incidentDetection: IncidentDetection,
  generation: String,
  zonesCount: Int,
  skills: Vector[String],
  christmasModeEnabled: Boolean,
  showAutoAssistReminders: Boolean,
  contactDetails: ContactDetails,
  address: Address,
  geolocation: Geolocation,
  consentGrantSkippable: Boolean,
  enabledFeatures: Vector[String],
  isAirComfortEligible: Boolean,
  isBalanceAcEligible: Boolean,
  isBalanceHpEligible: Boolean,
  isEnergyIqEligible: Boolean,
  isHeatSourceInstalled: Boolean,
) extends TadoResponse derives Decoder

object HomeResponse:
  given [F[_]](using Async[F]): EntityDecoder[F, HomeResponse] = jsonOf[F, HomeResponse]

/**
 * HomeZonesResponse
 */
final case class HomeZonesResponse(
  id: Int,
  name: String,
  `type`: String,
  dateCreated: String,
  deviceTypes: Vector[String],
  devices: Vector[Devices],
  reportAvailable: Boolean,
  showScheduleSetup: Boolean,
  supportsDazzle: Boolean,
  dazzleEnabled: Boolean,
  dazzleMode: DazzleMode,
  openWindowDetection: OpenWindowDetection,
) extends TadoResponse derives Decoder

object HomeZonesResponse:
  given [F[_]](using Async[F]): EntityDecoder[F, HomeZonesResponse] = jsonOf[F, HomeZonesResponse]

//  ----  //

/**
 * BearingFromHome
 */
final case class BearingFromHome(
  degrees: Double,
  radians: Double,
) derives Decoder

object BearingFromHome:
  given [F[_]](using Async[F]): EntityDecoder[F, BearingFromHome] = jsonOf[F, BearingFromHome]

/**
 * DeviceMetadata
 */
final case class DeviceMetadata(
  platform: String,
  osVersion: String,
  model: String,
  locale: String,
) derives Decoder

object DeviceMetadata:
  given [F[_]](using Async[F]): EntityDecoder[F, DeviceMetadata] = jsonOf[F, DeviceMetadata]

/**
 * Home
 */
final case class Home(
  id: Int,
  name: String,
) derives Decoder

object Home:
  given [F[_]](using Async[F]): EntityDecoder[F, Home] = jsonOf[F, Home]

/**
 * Location
 */
final case class Location(
  stale: Boolean,
  atHome: Boolean,
  bearingFromHome: BearingFromHome,
  relativeDistanceFromHomeFence: Int,
) derives Decoder

object Location:
  given [F[_]](using Async[F]): EntityDecoder[F, Location] = jsonOf[F, Location]

/**
 * MobileDevices
 */
final case class MobileDevices(
  name: String,
  id: Int,
  settings: Settings,
  location: Location,
  deviceMetadata: DeviceMetadata,
) derives Decoder

object MobileDevices:
  given [F[_]](using Async[F]): EntityDecoder[F, MobileDevices] = jsonOf[F, MobileDevices]

/**
 * PushNotifications
 */
final case class PushNotifications(
  lowBatteryReminder: Boolean,
  awayModeReminder: Boolean,
  homeModeReminder: Boolean,
  openWindowReminder: Boolean,
  energySavingsReportReminder: Boolean,
  incidentDetection: Boolean,
  energyIqReminder: Boolean,
) derives Decoder

object PushNotifications:
  given [F[_]](using Async[F]): EntityDecoder[F, PushNotifications] = jsonOf[F, PushNotifications]

/**
 * Settings
 */
final case class Settings(
  geoTrackingEnabled: Boolean,
  specialOffersEnabled: Boolean,
  onDemandLogRetrievalEnabled: Boolean,
  pushNotifications: PushNotifications,
) derives Decoder

object Settings:
  given [F[_]](using Async[F]): EntityDecoder[F, Settings] = jsonOf[F, Settings]

/**
 * Address
 */
final case class Address(
  addressLine1: String,
  addressLine2: Option[String],
  zipCode: String,
  city: String,
  state: Option[String],
  country: String,
) derives Decoder

object Address:
  given [F[_]](using Async[F]): EntityDecoder[F, Address] = jsonOf[F, Address]

/**
 * ContactDetails
 */
final case class ContactDetails(
  name: String,
  email: String,
  phone: String,
) derives Decoder

object ContactDetails:
  given [F[_]](using Async[F]): EntityDecoder[F, ContactDetails] = jsonOf[F, ContactDetails]

/**
 * Geolocation
 */
final case class Geolocation(
  latitude: Double,
  longitude: Double,
) derives Decoder

object Geolocation:
  given [F[_]](using Async[F]): EntityDecoder[F, Geolocation] = jsonOf[F, Geolocation]

/**
 * IncidentDetection
 */
final case class IncidentDetection(
  supported: Boolean,
  enabled: Boolean,
) derives Decoder

object IncidentDetection:
  given [F[_]](using Async[F]): EntityDecoder[F, IncidentDetection] = jsonOf[F, IncidentDetection]

/**
 * Characteristics
 */
final case class Characteristics(
  capabilities: Vector[String],
) derives Decoder

object Characteristics:
  given [F[_]](using Async[F]): EntityDecoder[F, Characteristics] = jsonOf[F, Characteristics]

/**
 * ConnectionState
 */
final case class ConnectionState(
  value: String,
  timestamp: String,
) derives Decoder

object ConnectionState:
  given [F[_]](using Async[F]): EntityDecoder[F, ConnectionState] = jsonOf[F, ConnectionState]

/**
 * DazzleMode
 */
final case class DazzleMode(
  supported: Boolean,
  enabled: Boolean,
) derives Decoder

object DazzleMode:
  given [F[_]](using Async[F]): EntityDecoder[F, DazzleMode] = jsonOf[F, DazzleMode]

/**
 * Devices
 */
final case class Devices(
  deviceType: String,
  serialNo: String,
  shortSerialNo: String,
  currentFwVersion: String,
  connectionState: ConnectionState,
  characteristics: Characteristics,
  batteryState: String,
  duties: Vector[String],
  mountingState: ConnectionState,
  mountingStateWithError: String,
  orientation: String,
  childLockEnabled: Boolean,
  isDriverConfigured: Option[Boolean],
) derives Decoder

object Devices:
  given [F[_]](using Async[F]): EntityDecoder[F, Devices] = jsonOf[F, Devices]

/**
 * OpenWindowDetection
 */
final case class OpenWindowDetection(
  supported: Boolean,
  enabled: Boolean,
  timeoutInSeconds: Int,
) derives Decoder

object OpenWindowDetection:
  given [F[_]](using Async[F]): EntityDecoder[F, OpenWindowDetection] = jsonOf[F, OpenWindowDetection]
