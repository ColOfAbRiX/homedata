// package com.colofabrix.scala.homedata.octopus

// import sttp.client4.UriContext
// import sttp.model.Uri
// import pureconfig.*
// import pureconfig.generic.derivation.default.*

// final case class OctopusConfig(
//   accountNumber: String,
//   baseUrl: String,
//   apiKey: String,
//   electricityMpan: String,
//   electricitySerial: String,
//   gasMprn: String,
//   gasSerial: String
// ) derives ConfigReader

// object OctopusConfig:

//   val config =
//     ConfigSource
//       .default
//       .withFallback(ConfigSource.resources("secrets.conf"))
//       .at("octopus")
//       .loadOrThrow[OctopusConfig]

//   val PageSize: Int =
//     100

//   val ElectricityConsumptionUrl: Uri =
//     uri"${config.baseUrl}/v1/electricity-meter-points/${config.electricityMpan}/meters/${config.electricitySerial}/consumption/"

//   val GasConsumptionUrl: Uri =
//     uri"${config.baseUrl}/v1/gas-meter-points/${config.gasMprn}/meters/${config.gasSerial}/consumption/"
