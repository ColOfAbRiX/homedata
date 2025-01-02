package com.colofabrix.scala.tado4s

import org.http4s.Uri
import pureconfig.*
import pureconfig.generic.derivation.default.*
import scala.concurrent.duration.*

final case class TadoConfig(
  apiBase: Uri,
  apiAuth: Uri,
  clientSecret: String,
  maxRetries: Int,
  maxRetryTime: FiniteDuration,
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
      .at("tado")
      .loadOrThrow[TadoConfig]
