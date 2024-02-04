package com.colofabrix.scala.homedata

import com.colofabrix.scala.timeflux.config.*
import io.github.arainko.ducktape.*
import org.http4s.Uri
import pureconfig.*
import pureconfig.generic.derivation.default.*

final case class InfluxDbConfig(
  serverUri: Uri,
  organizationId: OrganizationId,
  authToken: AuthToken,
  octopusBucket: String,
)

object InfluxDB:

  final private case class InfluxDbReaderConfig(
    serverUri: String,
    organizationId: String,
    authToken: String,
    octopusBucket: String,
  ) derives ConfigReader

  val config =
    ConfigSource
      .default
      .withFallback(ConfigSource.resources("secrets.conf"))
      .at("influxdb")
      .loadOrThrow[InfluxDbReaderConfig]
      .into[InfluxDbConfig]
      .transform(
        Field.computed(_.serverUri, c => Uri.unsafeFromString(c.serverUri)),
        Field.computed(_.organizationId, c => OrganizationId(c.organizationId)),
        Field.computed(_.authToken, c => AuthToken(c.authToken)),
      )
