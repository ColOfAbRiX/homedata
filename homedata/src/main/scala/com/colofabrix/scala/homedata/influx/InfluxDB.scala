package com.colofabrix.scala.homedata.influx

import com.colofabrix.scala.timeflux.config.*
import com.colofabrix.scala.timeflux.model.*
import io.github.arainko.ducktape.*
import org.http4s.Uri
import pureconfig.*
import pureconfig.generic.derivation.default.*

final case class InfluxDbConfig(
  serverUrl: Uri,
  orgId: OrgId,
  authToken: AuthToken,
  octopusBucket: String,
)

object InfluxDB:

  final private case class InfluxDbReaderConfig(
    serverUrl: String,
    orgId: String,
    authToken: String,
    octopusBucket: String,
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
      )

  val timefluxClientConfig: TimefluxClientConfig =
    config
      .into[TimefluxClientConfig]
      .transform()
