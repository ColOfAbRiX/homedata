package com.colofabrix.scala.homedata.tado

import pureconfig.*
import pureconfig.generic.derivation.default.*

final case class TadoConfig(
  requestsPerSec: Double,
) derives ConfigReader

object TadoConfig:

  val config =
    ConfigSource
      .default
      .withFallback(ConfigSource.resources("secrets.conf"))
      .at("tado")
      .loadOrThrow[TadoConfig]
