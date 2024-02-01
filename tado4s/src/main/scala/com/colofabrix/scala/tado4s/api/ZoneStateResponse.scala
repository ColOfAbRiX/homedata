package com.colofabrix.scala.tado4s.api

import io.circe.Decoder
import java.time.*

/**
 * ZoneStateResponse
 */
final case class ZoneStateResponse(
  tadoMode: String,
  geolocationOverride: Boolean,
  setting: ZoneStateResponse.Setting,
  nextScheduleChange: ZoneStateResponse.NextScheduleChange,
  link: ZoneStateResponse.Link,
  activityDataPoints: ZoneStateResponse.ActivityDataPoints,
  sensorDataPoints: ZoneStateResponse.SensorDataPoints,
) derives Decoder

object ZoneStateResponse:

  final case class ActivityDataPoints(
    heatingPower: Option[HeatingPower],
  ) derives Decoder

  final case class HeatingPower(
    `type`: String,
    percentage: Double,
    timestamp: OffsetDateTime,
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
    timestamp: OffsetDateTime,
    `type`: String,
    precision: Temperature,
  ) derives Decoder
