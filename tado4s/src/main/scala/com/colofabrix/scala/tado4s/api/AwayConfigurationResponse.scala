package com.colofabrix.scala.tado4s.api

import io.circe.Decoder
import io.circe.Encoder

/** Response for away configuration */
final case class AwayConfigurationResponse(
  `type`: String,
  autoAdjust: Boolean,
  comfortLevel: Int,
  setting: AwayConfigurationResponse.Setting,
) derives Decoder, Encoder.AsObject

object AwayConfigurationResponse {

  final case class Setting(
    `type`: String,
    power: String,
    temperature: Option[Temperature],
  ) derives Decoder, Encoder.AsObject

  final case class Temperature(
    celsius: Double,
    fahrenheit: Double,
  ) derives Decoder, Encoder.AsObject

}
