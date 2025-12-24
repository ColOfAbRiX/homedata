package com.colofabrix.scala.cuttlefish.api

import com.colofabrix.scala.cuttlefish.model.Mpan

/** Request for the electricity meter point endpoint */
final case class ElectricityMeterPointRequest(
  mpan: Mpan,
)
