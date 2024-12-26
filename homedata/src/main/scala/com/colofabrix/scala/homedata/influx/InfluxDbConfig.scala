package com.colofabrix.scala.homedata.influx

import com.colofabrix.scala.timeflux.config.*
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
)

object InfluxDbConfig:

  final private case class InfluxDbReaderConfig(
    serverUrl: String,
    orgId: String,
    authToken: String,
    projectBucket: String,
    timeResolution: String,
  ) derives ConfigReader

  val config: InfluxDbConfig =
    ConfigSource
      .default
      .withFallback(ConfigSource.resources("secrets.conf"))
      .at("influxdb")
      .loadOrThrow[InfluxDbReaderConfig]
      .into[InfluxDbConfig]
      .transform(
        Field.computed(_.serverUrl, c => Uri.unsafeFromString(c.serverUrl)),
        Field.computed(_.orgId, c => OrgId(c.orgId)),
        Field.computed(_.authToken, c => AuthToken(c.authToken)),
        Field.computed(_.timeResolution, c => toFiniteDuration(c.timeResolution)),
      )

  private def toFiniteDuration(value: String): FiniteDuration =
    Some(Duration(value))
      .collect { case fd: FiniteDuration => fd }
      .get

  val clientConfig: TimefluxClientConfig =
    config
      .into[TimefluxClientConfig]
      .transform()
