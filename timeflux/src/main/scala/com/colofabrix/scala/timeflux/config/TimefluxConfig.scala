package com.colofabrix.scala.timeflux.config

import io.github.arainko.ducktape.*
import org.http4s.Uri
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
