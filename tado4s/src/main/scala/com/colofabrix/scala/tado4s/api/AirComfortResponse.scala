package com.colofabrix.scala.tado4s.api

import io.circe.Decoder

/** Response for air comfort */
final case class AirComfortResponse(
  freshness: AirComfortResponse.Freshness,
  comfort: Vector[AirComfortResponse.ZoneComfort],
) derives Decoder

object AirComfortResponse {

  final case class Freshness(
    value: String,
    lastOpenWindow: Option[String],
  ) derives Decoder

  final case class ZoneComfort(
    roomId: Int,
    temperatureLevel: String,
    humidityLevel: String,
    coordinate: Coordinate,
  ) derives Decoder

  final case class Coordinate(
    radial: Double,
    angular: Double,
  ) derives Decoder

}
