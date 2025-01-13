package com.colofabrix.scala.timeflux.config

import pureconfig.*
import pureconfig.generic.derivation.default.*
import scala.concurrent.duration.*

/**
 * Startup configuration of the Timeflux Client
 *
 * @param apiBase Base URL for the API calls
 * @param concurrentWrites Number of concurrent batched writes for streaming requests
 * @param httpTimeout HTTP Timeout
 * @param maxRetries Max number of retries for HTTP requests
 * @param maxRetryTime Maximum retry time
 */
final case class TimefluxConfig(
  apiBase: String,
  concurrentWrites: Int = 5,
  httpTimeout: FiniteDuration = 30.seconds,
  maxRetries: Int = 5,
  maxRetryTime: FiniteDuration = 1.minute,
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
