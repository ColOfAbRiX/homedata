package com.colofabrix.scala.tado4s.api

import io.circe.Encoder

/** Request to set a zone overlay (manual control) */
final case class SetZoneOverlayRequest(
  homeId: Int,
  zoneId: Int,
  setting: ZoneOverlayRequest.Setting,
  termination: ZoneOverlayRequest.Termination,
)

object ZoneOverlayRequest {

  final case class Setting(
    `type`: String,
    power: String,
    temperature: Option[Temperature],
  ) derives Encoder.AsObject

  final case class Temperature(
    celsius: Option[Double],
    fahrenheit: Option[Double],
  ) derives Encoder.AsObject

  final case class Termination(
    `type`: String,
    durationInSeconds: Option[Int],
  ) derives Encoder.AsObject

}

/** Request to delete a zone overlay */
final case class DeleteZoneOverlayRequest(
  homeId: Int,
  zoneId: Int,
)
