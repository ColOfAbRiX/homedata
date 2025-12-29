package com.colofabrix.scala.tado4s.api

import io.circe.Decoder

/** Response for zone overlay */
final case class ZoneOverlayResponse(
  `type`: String,
  setting: ZoneOverlayResponse.Setting,
  termination: ZoneOverlayResponse.Termination,
) derives Decoder

object ZoneOverlayResponse {

  final case class Setting(
    `type`: String,
    power: String,
    temperature: Option[Temperature],
  ) derives Decoder

  final case class Temperature(
    celsius: Double,
    fahrenheit: Double,
  ) derives Decoder

  final case class Termination(
    `type`: String,
    typeSkillBasedApp: Option[String],
    durationInSeconds: Option[Int],
    expiry: Option[String],
    remainingTimeInSeconds: Option[Int],
    projectedExpiry: Option[String],
  ) derives Decoder

}
