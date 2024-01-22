package com.colofabrix.scala.timeflux.config

import org.http4s.Uri

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
