package com.colofabrix.scala.homedata.octopus

import com.colofabrix.scala.cuttlefish.model.*
import pureconfig.*
import pureconfig.generic.derivation.default.*
import sttp.client4.UriContext
import sttp.model.Uri

final case class OctopusConfig(
  accountNumber: String,
  baseUrl: String,
  apiKey: String,
  electricityMpan: MeterPointNumber,
  electricitySerial: SerialNumber,
  gasMprn: MeterPointNumber,
  gasSerial: SerialNumber,
  requestsPerSec: Double,
) derives ConfigReader

object OctopusConfig:

  val config =
    ConfigSource
      .default
      .withFallback(ConfigSource.resources("secrets.conf"))
      .at("octopus")
      .loadOrThrow[OctopusConfig]

  val PageSize: Int =
    100

  val ElectricityConsumptionUrl: Uri =
    uri"${config.baseUrl}/v1/electricity-meter-points/${config.electricityMpan}/meters/${config.electricitySerial}/consumption/"

  val GasConsumptionUrl: Uri =
    uri"${config.baseUrl}/v1/gas-meter-points/${config.gasMprn.value}/meters/${config.gasSerial.value}/consumption/"

  given ConfigReader[MeterPointNumber] =
    ConfigReader.fromString: str =>
      Right(MeterPointNumber(str))

  given ConfigReader[SerialNumber] =
    ConfigReader.fromString: str =>
      Right(SerialNumber(str))
