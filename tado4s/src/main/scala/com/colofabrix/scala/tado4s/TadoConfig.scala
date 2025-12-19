package com.colofabrix.scala.tado4s

import org.http4s.Uri
import pureconfig.*
import pureconfig.generic.derivation.default.*
import scala.concurrent.duration.*

/**
 * Startup configuration of the Tado4s Client
 *
 * @param apiBase Base URL for the API calls
 * @param apiAuth OAuth2 Authentication URL
 * @param clientId OAuth2 Client ID
 * @param refreshToken OAuth2 Refresh Token
 * @param httpTimeout HTTP Timeout
 * @param maxRetries Max number of retries for HTTP requests
 * @param maxRetryTime Maximum retry time
 * @param ignoreSsl Setting to disable SSL endpoint verification
 */
final case class TadoConfig(
  apiBase: Uri,
  apiAuth: Uri,
  clientId: String,
  refreshToken: Option[String] = None,
  httpTimeout: FiniteDuration = 30.seconds,
  maxRetries: Int = 5,
  maxRetryTime: FiniteDuration = 1.minute,
  ignoreSsl: Boolean = true
) derives ConfigReader

/**
 * Tado4s configuration
 */
object TadoConfig:

  given ConfigReader[FiniteDuration] =
    ConfigReader.fromString:
      ConvertHelpers.optF: str =>
        Some(Duration(str)).collect { case fd: FiniteDuration => fd }

  given ConfigReader[Uri] =
    ConfigReader.fromString:
      ConvertHelpers.tryF: str =>
        Uri.fromString(str).toTry

  val config: TadoConfig =
    ConfigSource
      .default
      .withFallback(ConfigSource.resources("secrets.conf"))
      .at("tado")
      .loadOrThrow[TadoConfig]
