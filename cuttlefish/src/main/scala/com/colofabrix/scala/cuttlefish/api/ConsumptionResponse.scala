package com.colofabrix.scala.cuttlefish.api

import io.circe.Decoder
import java.time.OffsetDateTime

final case class MeterConsumptionResponse(
  count: Int,
  next: Option[String],
  previous: Option[String],
  results: List[ConsumptionResults],
) derives Decoder

final case class ConsumptionResults(
  consumption: Double,
  interval_start: OffsetDateTime,
  interval_end: OffsetDateTime,
) derives Decoder
