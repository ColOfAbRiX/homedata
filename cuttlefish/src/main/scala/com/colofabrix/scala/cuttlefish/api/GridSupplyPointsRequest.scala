package com.colofabrix.scala.cuttlefish.api

import com.colofabrix.scala.cuttlefish.model.Postcode

/** Request for the grid supply points endpoint */
final case class GridSupplyPointsRequest(
  postcode: Postcode,
)
