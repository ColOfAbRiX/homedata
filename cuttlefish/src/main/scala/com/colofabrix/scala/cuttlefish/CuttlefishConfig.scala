package com.colofabrix.scala.cuttlefish

import org.http4s.Uri
import pureconfig.*
import pureconfig.generic.derivation.default.*
import scala.concurrent.duration.*

final case class CuttlefishConfig(
  apiBase: Uri,
  maxRetries: Int,
  maxRetryTime: FiniteDuration,
) derives ConfigReader

object CuttlefishConfig:

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
