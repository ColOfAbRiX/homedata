package com.colofabrix.scala.cuttlefish.api

import io.circe.Decoder

final case class MeterConsumptionResponse (
  count: Int,
  next: Option[String],
  previous: Option[String],
  results: List[Results]
) derives Decoder

final case class Results (
  consumption: Double,
  interval_start: String,
  interval_end: String
) derives Decoder
