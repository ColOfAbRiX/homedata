// package com.colofabrix.scala.homedata.tado

// import sttp.client4.UriContext
// import sttp.model.Uri
// import pureconfig.*
// import pureconfig.generic.derivation.default.*

// final case class TadoConfig(
//   myUrl: Uri,
//   authUrl: Uri,
//   clientSecret: String,
// ) derives ConfigReader

// object TadoConfig:

//   final private case class TadoReaderConfig(
//     myUrl: String,
//     authUrl: String,
//     clientSecret: String,
//   ) derives ConfigReader

//   val config =
//     ConfigSource
//       .default
//       .withFallback(ConfigSource.resources("secrets.conf"))
//       .at("tado")
//       .loadOrThrow[TadoConfig]
