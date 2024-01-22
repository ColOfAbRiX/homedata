package com.colofabrix.scala.timeflux.config

import org.http4s.Uri

/**
  * Client configuration
  *
  * @param serverUrl URL of the InfluxDB server
  * @param token Authentication token
  * @param organizationId Organization ID (not the name!)
  */
final case class TimefluxClientConfig(
  serverUrl: Uri,
  token: AuthToken,
  organizationId: OrganizationId,
)

opaque type AuthToken = String
object AuthToken:
  extension (self: AuthToken) def value: String = self
  def apply(value: String): AuthToken =
    value

opaque type OrganizationId = String
object OrganizationId:
  extension (self: OrganizationId) def value: String = self
  def apply(value: String): OrganizationId =
    value
