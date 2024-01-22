package com.colofabrix.scala.homedata.octopus

import sttp.client4.UriContext
import sttp.model.Uri

object OctopusConfig:

  val AccountNumber: String     = ""
  val BaseUrl: String           = ""
  val ApiKey: String            = ""
  val ElectricityMpan: String   = ""
  val ElectricitySerial: String = ""
  val GasMprn: String           = ""
  val GasSerial: String         = ""
  val PageSize: Int             = 500

  val ElectricityConsumptionUrl: Uri =
    uri"$BaseUrl/v1/electricity-meter-points/$ElectricityMpan/meters/$ElectricitySerial/consumption/"

  val GasConsumptionUrl: Uri =
    uri"$BaseUrl/v1/gas-meter-points/${GasMprn}/meters/${GasSerial}/consumption/"
