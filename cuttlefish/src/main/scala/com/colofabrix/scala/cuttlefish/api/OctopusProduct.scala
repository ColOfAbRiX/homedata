package com.colofabrix.scala.cuttlefish.api

enum OctopusProduct(val value: String):
  case Electricity extends OctopusProduct("electricity")
  case Gas         extends OctopusProduct("gas")
