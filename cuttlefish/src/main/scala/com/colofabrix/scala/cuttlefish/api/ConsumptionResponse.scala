package com.colofabrix.scala.cuttlefish

final case class ConsumptionResponse (
  count: Int,
  next: String,
  previous: String,
  results: List[Results]
)

final case class Results (
  consumption: Double,
  interval_start: String,
  interval_end: String
)
