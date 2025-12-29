package com.colofabrix.scala.tado4s.api

import io.circe.Decoder
import io.circe.Encoder

/** Response for active timetable */
final case class ActiveTimetableResponse(
  id: Int,
  `type`: String,
) derives Decoder, Encoder.AsObject

/** Response for listing all timetables */
final case class TimetablesResponse(
  timetables: Vector[TimetablesResponse.Timetable],
) derives Decoder

object TimetablesResponse {

  final case class Timetable(
    id: Int,
    `type`: String,
  ) derives Decoder

}

/** Response for timetable blocks */
final case class TimetableBlocksResponse(
  blocks: Vector[TimetableBlocksResponse.Block],
) derives Decoder

object TimetableBlocksResponse {

  final case class Block(
    dayType: String,
    start: String,
    end: String,
    geolocationOverride: Boolean,
    setting: Setting,
  ) derives Decoder, Encoder.AsObject

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
