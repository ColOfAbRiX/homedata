package com.colofabrix.scala.homedata.influx

import com.colofabrix.scala.homedata.utils.commongivens.given
import com.colofabrix.scala.timeflux.config.TimefluxClientConfig
import com.colofabrix.scala.timeflux.model.*
import org.http4s.Uri
import pureconfig.*
import pureconfig.generic.derivation.default.*
import scala.concurrent.duration.*

final case class InfluxConfig(
  serverUrl: Uri,
  orgName: OrgName,
  authToken: AuthToken,
  projectBucket: String,
  timeResolution: FiniteDuration,
  batchWrites: Int,
) derives ConfigReader

object InfluxConfig {

  given ConfigReader[OrgName] =
    ConfigReader.fromString: str =>
      Right(OrgName(str))

  given ConfigReader[AuthToken] =
    ConfigReader.fromString: str =>
      Right(AuthToken(str))

  val config: InfluxConfig =
    ConfigSource
      .default
      .at("homedata.influxdb")
      .loadOrThrow[InfluxConfig]

  val clientConfig: TimefluxClientConfig =
    TimefluxClientConfig(
      serverUrl = config.serverUrl,
      authToken = config.authToken,
    )

}
