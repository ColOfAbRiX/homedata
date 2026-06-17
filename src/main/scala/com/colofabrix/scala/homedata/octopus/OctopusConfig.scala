package com.colofabrix.scala.homedata.octopus

import cats.syntax.*
import com.colofabrix.scala.cuttlefish.models.*
import pureconfig.*

final case class OctopusConfig(
  accountNumber: String,
  apiKey: String,
  electricityMpan: Mpan,
  electricitySerial: MeterSerial,
  gasMprn: Mprn,
  gasSerial: MeterSerial,
  requestsPerSec: Double,
  pageSize: PageSize,
) derives ConfigReader

object OctopusConfig {

  val config =
    ConfigSource
      .default
      .at("homedata.octopus")
      .loadOrThrow[OctopusConfig]

  given ConfigReader[Mpan] =
    ConfigReader.fromStringOpt: str =>
      Mpan.refined[Either[Throwable, *]](str).toOption

  given ConfigReader[Mprn] =
    ConfigReader.fromStringOpt: str =>
      Mprn.refined[Either[Throwable, *]](str).toOption

  given ConfigReader[MeterSerial] =
    ConfigReader.fromStringOpt: str =>
      MeterSerial.refined[Either[Throwable, *]](str).toOption

  given ConfigReader[PageSize] =
    ConfigReader.fromStringOpt: str =>
      for {
        int  <- str.toIntOption
        page <- PageSize.refined[Either[Throwable, *]](int).toOption
      } yield page

}
