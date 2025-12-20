package com.colofabrix.scala.timeflux.api

import io.circe.*
import io.circe.derivation.*
import java.time.OffsetDateTime

/**
 * Create Organization Response
 */
final case class CreateOrgResponse(
  id: String,
  name: String,
  description: Option[String],
  createdAt: Option[OffsetDateTime],
  updatedAt: Option[OffsetDateTime],
  links: Option[Links],
) derives Decoder
