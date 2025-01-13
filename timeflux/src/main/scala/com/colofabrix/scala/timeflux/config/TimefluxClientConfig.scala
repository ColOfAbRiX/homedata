package com.colofabrix.scala.timeflux.config

import org.http4s.Uri
import com.colofabrix.scala.timeflux.model.*

/**
 * Runtime configuration of the Timeflux Client
 *
 * @param serverUrl Influxdb server to connect to
 * @param authToken Influxdb Authentication Token
 * @param orgId Influxdb Organization ID
 */
final case class TimefluxClientConfig(
  serverUrl: Uri,
  authToken: AuthToken,
  orgId: OrgId,
)
