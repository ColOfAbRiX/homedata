package com.colofabrix.scala.timeflux.config

import org.http4s.Uri
import com.colofabrix.scala.timeflux.model.*

final case class TimefluxClientConfig(
  serverUrl: Uri,
  authToken: AuthToken,
  orgId: OrgId,
)
