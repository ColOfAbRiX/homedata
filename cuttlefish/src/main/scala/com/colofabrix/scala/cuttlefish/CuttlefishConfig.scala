package com.colofabrix.scala.cuttlefish

import org.http4s.Uri
import pureconfig.*
import pureconfig.generic.derivation.default.*

final case class CuttlefishConfig(
  apiBase: Uri
) derives ConfigReader

object CuttlefishConfig:

  given ConfigReader[Uri] =
    ConfigReader.fromString:
      ConvertHelpers.tryF: str =>
        Uri.fromString(str).toTry

  val config =
    ConfigSource
      .default
      .withFallback(ConfigSource.resources("secrets.conf"))
      .at("cuttlefish")
      .loadOrThrow[CuttlefishConfig]
