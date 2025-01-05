package com.colofabrix.scala.timeflux.config

import pureconfig.*
import pureconfig.generic.derivation.default.*
import scala.concurrent.duration.*

/**
 * Client configuration
 */
final case class TimefluxConfig(
  apiBase: String,
  concurrentWrites: Int,
  maxRetries: Int,
  maxRetryTime: FiniteDuration,
) derives ConfigReader

object TimefluxConfig:

  given ConfigReader[FiniteDuration] =
    ConfigReader.fromString:
      ConvertHelpers.optF: str =>
        Some(Duration(str)).collect { case fd: FiniteDuration => fd }

  val config: TimefluxConfig =
    ConfigSource
      .default
      .at("timeflux")
      .loadOrThrow[TimefluxConfig]
