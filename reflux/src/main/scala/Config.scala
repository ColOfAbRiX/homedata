package reflux

import org.http4s.Uri

object config:

  final case class InfluxClientConfig(
    serverUrl: Uri,
    token: InfluxAuthToken,
    organizationId: OrganizationId,
  )

  opaque type InfluxAuthToken =
    String

  object InfluxAuthToken:
    extension (self: InfluxAuthToken) def value: String = self
    def apply(value: String): InfluxAuthToken =
      value

  opaque type OrganizationId =
    String

  object OrganizationId:
    extension (self: OrganizationId) def value: String = self
    def apply(value: String): OrganizationId =
      value
