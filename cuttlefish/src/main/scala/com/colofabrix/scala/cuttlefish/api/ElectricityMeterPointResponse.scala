package com.colofabrix.scala.cuttlefish.api

import io.circe.Decoder

/** Response from the electricity meter point endpoint */
final case class ElectricityMeterPointResponse(
  gsp: String,
  mpan: String,
  profile_class: Int,
) derives Decoder
