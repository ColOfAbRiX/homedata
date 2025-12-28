package com.colofabrix.scala.cuttlefish

import org.http4s.Uri
import pureconfig.*
import pureconfig.generic.derivation.default.*
import scala.concurrent.duration.*

/**
 * Startup configuration of the Cuttlefish Client
 *
 * @param apiBase Base URL for the API calls
 * @param httpTimeout HTTP Timeout
 * @param maxRetries Max number of retries for HTTP requests
 * @param maxRetryTime Maximum retry time
 */
final case class CuttlefishConfig(
  apiBase: Uri,
  httpTimeout: FiniteDuration = 30.seconds,
  maxRetries: Int = 5,
  maxRetryTime: FiniteDuration = 1.minute,
) derives ConfigReader

object CuttlefishConfig {

  given ConfigReader[Uri] =
    ConfigReader.fromString:
      ConvertHelpers.tryF: str =>
        Uri.fromString(str).toTry

  given ConfigReader[FiniteDuration] =
    ConfigReader.fromString:
      ConvertHelpers.optF: str =>
        Some(Duration(str)).collect { case fd: FiniteDuration => fd }

  val config =
    ConfigSource
      .default
      .withFallback(ConfigSource.resources("secrets.conf"))
      .at("cuttlefish")
      .loadOrThrow[CuttlefishConfig]

}
