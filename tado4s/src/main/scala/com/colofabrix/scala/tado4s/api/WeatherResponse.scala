package com.colofabrix.scala.tado4s.api

import io.circe.Decoder
import java.time.*

/**
 * WeatherResponse
 */
final case class WeatherResponse(
  solarIntensity: WeatherResponse.SolarIntensity,
  outsideTemperature: WeatherResponse.OutsideTemperature,
  weatherState: WeatherResponse.WeatherState,
) derives Decoder

object WeatherResponse:

  final case class OutsideTemperature(
    celsius: Double,
    fahrenheit: Double,
    timestamp: Instant,
    `type`: String,
    precision: Temperature,
  ) derives Decoder

  final case class Temperature(
    celsius: Double,
    fahrenheit: Double,
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
