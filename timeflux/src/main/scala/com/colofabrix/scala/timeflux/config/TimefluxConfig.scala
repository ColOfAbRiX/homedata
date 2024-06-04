package com.colofabrix.scala.timeflux.config

import pureconfig.*
import pureconfig.generic.derivation.default.*

/**
  * Client configuration
  */
final case class TimefluxConfig(
  apiBase: String
) derives ConfigReader

object TimefluxConfig:

  val config: TimefluxConfig =
    ConfigSource
      .default
      .at("timeflux")
      .loadOrThrow[TimefluxConfig]
