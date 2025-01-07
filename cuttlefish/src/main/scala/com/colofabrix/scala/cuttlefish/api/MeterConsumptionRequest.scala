package com.colofabrix.scala.cuttlefish.api

import com.colofabrix.scala.cuttlefish.model.*
import java.time.OffsetDateTime

final case class MeterConsumptionRequest(
  product: OctopusProduct,
  meterPointNumber: MeterPointNumber,
  serial: SerialNumber,
  from: Option[OffsetDateTime],
  to: Option[OffsetDateTime],
  pageSize: Option[Int],
  page: Option[Int],
  orderBy: Option[String],
)
