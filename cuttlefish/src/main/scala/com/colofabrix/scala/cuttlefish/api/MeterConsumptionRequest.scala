package com.colofabrix.scala.cuttlefish.api

import java.time.OffsetDateTime

final case class MeterConsumptionRequest(
  from: Option[OffsetDateTime],
  to: Option[OffsetDateTime],
  pageSize: Option[Int],
  orderBy: Option[String],
  serial: String,
  meterPointNumber: String,
  product: OctopusProduct,
)
