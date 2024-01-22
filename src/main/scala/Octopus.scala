package com.colofabrix.scala.homedata

import sttp.client4.*

object Octopus:
  val AccountNumber     = ""
  val BaseUrl           = ""
  val ApiKey            = ""
  val ElectricityMpan   = ""
  val ElectricitySerial = ""
  val GasMprn           = ""
  val GasSerial         = ""

  val ElectricityConsumptionUrl =
    uri"$BaseUrl/v1/electricity-meter-points/$ElectricityMpan/meters/$ElectricitySerial/consumption/"
