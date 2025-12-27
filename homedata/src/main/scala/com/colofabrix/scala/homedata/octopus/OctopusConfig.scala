package com.colofabrix.scala.homedata.octopus

import com.colofabrix.scala.cuttlefish.model.*
import pureconfig.*
import pureconfig.generic.derivation.default.*

final case class OctopusConfig(
  accountNumber: String,
  apiKey: String,
  electricityMpan: MeterPointNumber,
  electricitySerial: SerialNumber,
  gasMprn: MeterPointNumber,
  gasSerial: SerialNumber,
  requestsPerSec: Double,
  pageSize: Int,
) derives ConfigReader

object OctopusConfig:

  val config =
    ConfigSource
      .default
      .withFallback(ConfigSource.resources("secrets.conf"))
      .at("octopus")
      .loadOrThrow[OctopusConfig]

  given ConfigReader[MeterPointNumber] =
    ConfigReader.fromString: str =>
      Right(MeterPointNumber(str))

  given ConfigReader[SerialNumber] =
    ConfigReader.fromString: str =>
      Right(SerialNumber(str))
