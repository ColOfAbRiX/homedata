package com.colofabrix.scala.tado4s

import io.circe.Decoder
import java.time.*
import java.util.UUID

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
) derives Decoder

/**
 * AccountResponse
 */
final case class AccountResponse(
  name: String,
  email: String,
  username: String,
  id: UUID,
  homes: Vector[Home],
  locale: String,
  mobileDevices: Vector[MobileDevice],
) derives Decoder

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
  locale: String,
) derives Decoder

final case class Location(
  stale: Boolean,
  atHome: Boolean,
  bearingFromHome: BearingFromHome,
  relativeDistanceFromHomeFence: Int,
) derives Decoder

final case class BearingFromHome(
  degrees: Double,
  radians: Double,
) derives Decoder

final case class Settings(
  geoTrackingEnabled: Boolean,
  specialOffersEnabled: Boolean,
  onDemandLogRetrievalEnabled: Boolean,
  pushNotifications: PushNotifications,
) derives Decoder

final case class PushNotifications(
  lowBatteryReminder: Boolean,
  awayModeReminder: Boolean,
  homeModeReminder: Boolean,
  openWindowReminder: Boolean,
  energySavingsReportReminder: Boolean,
  incidentDetection: Boolean,
  energyIqReminder: Boolean,
) derives Decoder

/**
 * HomeResponse
 */
final case class HomeResponse(
  id: Int,
  name: String,
  dateTimeZone: String,
  dateCreated: Instant,
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
) derives Decoder

final case class Address(
  addressLine1: String,
  addressLine2: Option[String],
  zipCode: String,
  city: String,
  state: Option[String],
  country: String,
) derives Decoder

final case class ContactDetails(
  name: String,
  email: String,
  phone: String,
) derives Decoder

final case class Geolocation(
  latitude: Double,
  longitude: Double,
) derives Decoder

final case class IncidentDetection(
  supported: Boolean,
  enabled: Boolean,
) derives Decoder

/**
 * HomeZonesResponse
 */
final case class HomeZonesResponse(
  id: Int,
  name: String,
  `type`: String,
  dateCreated: Instant,
  deviceTypes: Vector[String],
  devices: Vector[Device],
  reportAvailable: Boolean,
  showScheduleSetup: Boolean,
  supportsDazzle: Boolean,
  dazzleEnabled: Boolean,
  dazzleMode: DazzleMode,
  openWindowDetection: OpenWindowDetection,
) derives Decoder

final case class DazzleMode(
  supported: Boolean,
  enabled: Option[Boolean],
) derives Decoder

final case class Device(
  deviceType: String,
  serialNo: String,
  shortSerialNo: String,
  currentFwVersion: String,
  connectionState: ConnectionState,
  characteristics: Characteristics,
  batteryState: Option[String],
  duties: Vector[String],
  mountingState: Option[MountingState],
  mountingStateWithError: Option[String],
  orientation: Option[String],
  childLockEnabled: Option[Boolean],
  isDriverConfigured: Option[Boolean],
) derives Decoder

final case class Characteristics(
  capabilities: Vector[String],
) derives Decoder

final case class ConnectionState(
  value: Boolean,
  timestamp: Instant,
) derives Decoder

final case class MountingState(
  value: String,
  timestamp: Instant,
) derives Decoder

final case class OpenWindowDetection(
  supported: Boolean,
  enabled: Option[Boolean],
  timeoutInSeconds: Option[Int],
) derives Decoder

/**
 * HomeStateResponse
 */
final case class HomeStateResponse(
  presence: String,
  presenceLocked: Boolean,
) derives Decoder

/**
 * ZoneStateResponse
 */
final case class ZoneStateResponse(
  tadoMode: String,
  geolocationOverride: Boolean,
  setting: Setting,
  nextScheduleChange: NextScheduleChange,
  link: Link,
  activityDataPoints: ActivityDataPoints,
  sensorDataPoints: SensorDataPoints,
) derives Decoder

final case class ActivityDataPoints(
  heatingPower: Option[HeatingPower],
) derives Decoder

final case class HeatingPower(
  `type`: String,
  percentage: Double,
  timestamp: Instant,
) derives Decoder

final case class Link(
  state: String,
) derives Decoder

final case class NextScheduleChange(
  start: String,
  setting: Setting,
) derives Decoder

final case class Setting(
  `type`: String,
  power: String,
  temperature: Option[Temperature],
) derives Decoder

final case class Temperature(
  celsius: Double,
  fahrenheit: Double,
) derives Decoder

final case class SensorDataPoints(
  insideTemperature: Option[InsideTemperature],
  humidity: Option[HeatingPower],
) derives Decoder

final case class InsideTemperature(
  celsius: Double,
  fahrenheit: Double,
  timestamp: Instant,
  `type`: String,
  precision: Temperature,
) derives Decoder

/**
 * WeatherResponse
 */
final case class WeatherResponse(
  solarIntensity: SolarIntensity,
  outsideTemperature: OutsideTemperature,
  weatherState: WeatherState,
) derives Decoder

final case class OutsideTemperature(
  celsius: Double,
  fahrenheit: Double,
  timestamp: Instant,
  `type`: String,
  precision: Temperature,
) derives Decoder

final case class SolarIntensity(
  `type`: String,
  percentage: Double,
  timestamp: Instant,
) derives Decoder

final case class WeatherState(
  `type`: String,
  value: String,
  timestamp: Instant,
) derives Decoder

/**
 * DayReportResponse
 */
final case class DayReportResponse(
  zoneType: String,
  interval: Interval,
  hoursInDay: Int,
  measuredData: MeasuredData,
  stripes: Stripes,
  settings: ReportSettings,
  callForHeat: CallForHeat,
  hotWaterProduction: ReportHotWaterProduction,
  weather: Weather,
) derives Decoder

final case class CallForHeat(
  timeSeriesType: String,
  valueType: String,
  dataIntervals: Vector[CallForHeatDataInterval],
) derives Decoder

final case class CallForHeatDataInterval(
  from: String,
  to: String,
  value: String,
) derives Decoder

final case class ReportHotWaterProduction(
  timeSeriesType: String,
  valueType: String,
  dataIntervals: Vector[HotWaterProductionDataInterval],
) derives Decoder

final case class HotWaterProductionDataInterval(
  from: String,
  to: String,
  value: Boolean,
) derives Decoder

final case class Interval(
  from: String,
  to: String,
) derives Decoder

final case class MeasuredData(
  measuringDeviceConnected: ReportHotWaterProduction,
  insideTemperature: ReportInsideTemperature,
  humidity: ReportHumidity,
) derives Decoder

final case class ReportHumidity(
  timeSeriesType: String,
  valueType: String,
  percentageUnit: String,
  min: Double,
  max: Double,
  dataPoints: Vector[HumidityDataPoint],
) derives Decoder

final case class HumidityDataPoint(
  timestamp: Instant,
  value: Double,
) derives Decoder

final case class ReportInsideTemperature(
  timeSeriesType: String,
  valueType: String,
  min: Max,
  max: Max,
  dataPoints: Vector[InsideTemperatureDataPoint],
) derives Decoder

final case class InsideTemperatureDataPoint(
  timestamp: Instant,
  value: Max,
) derives Decoder

final case class Max(
  celsius: Double,
  fahrenheit: Double,
) derives Decoder

final case class ReportSettings(
  timeSeriesType: String,
  valueType: String,
  dataIntervals: Vector[SettingsDataInterval],
) derives Decoder

final case class SettingsDataInterval(
  from: String,
  to: String,
  value: SettingClass,
) derives Decoder

final case class SettingClass(
  `type`: String,
  power: String,
  temperature: Max,
) derives Decoder

final case class Stripes(
  timeSeriesType: String,
  valueType: String,
  dataIntervals: Vector[StripesDataInterval],
) derives Decoder

final case class StripesDataInterval(
  from: String,
  to: String,
  value: PurpleValue,
) derives Decoder

final case class PurpleValue(
  stripeType: String,
  setting: SettingClass,
) derives Decoder

final case class Weather(
  condition: Condition,
  sunny: ReportHotWaterProduction,
  slots: Slots,
) derives Decoder

final case class Condition(
  timeSeriesType: String,
  valueType: String,
  dataIntervals: Vector[ConditionDataInterval],
) derives Decoder

final case class ConditionDataInterval(
  from: String,
  to: String,
  value: Slot,
) derives Decoder

final case class Slot(
  state: String,
  temperature: Max,
) derives Decoder

final case class Slots(
  timeSeriesType: String,
  valueType: String,
  slots: Map[String, Slot],
) derives Decoder
