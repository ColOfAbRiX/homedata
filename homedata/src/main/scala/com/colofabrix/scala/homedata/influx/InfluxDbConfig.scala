package com.colofabrix.scala.homedata.influx

import com.colofabrix.scala.timeflux.config.TimefluxClientConfig
import com.colofabrix.scala.timeflux.model.*
import io.github.arainko.ducktape.*
import org.http4s.Uri
import pureconfig.*
import pureconfig.generic.derivation.default.*
import scala.concurrent.duration.{ Duration, FiniteDuration }

final case class InfluxDbConfig(
  serverUrl: Uri,
  orgId: OrgId,
  authToken: AuthToken,
  projectBucket: String,
  timeResolution: FiniteDuration,
  batchWrites: Int,
) derives ConfigReader

object InfluxDbConfig:

  given ConfigReader[FiniteDuration] =
    ConfigReader.fromString:
      ConvertHelpers.optF: str =>
        Some(Duration(str)).collect { case fd: FiniteDuration => fd }

  given ConfigReader[Uri] =
    ConfigReader.fromString:
      ConvertHelpers.tryF: str =>
        Uri.fromString(str).toTry

  given ConfigReader[OrgId] =
    ConfigReader.fromString: str =>
      Right(OrgId(str))

  given ConfigReader[AuthToken] =
    ConfigReader.fromString: str =>
      Right(AuthToken(str))

  val config: InfluxDbConfig =
    ConfigSource
      .default
      .withFallback(ConfigSource.resources("secrets.conf"))
      .at("influxdb")
      .loadOrThrow[InfluxDbConfig]

  val clientConfig: TimefluxClientConfig =
    config
      .into[TimefluxClientConfig]
      .transform()
