package com.colofabrix.scala.cuttlefish.api

import io.circe.Decoder

/** Response from the grid supply points endpoint */
final case class GridSupplyPointsResponse(
  count: Int,
  next: Option[String],
  previous: Option[String],
  results: List[GridSupplyPoint],
) derives Decoder

/** A grid supply point */
final case class GridSupplyPoint(
  group_id: String,
) derives Decoder
