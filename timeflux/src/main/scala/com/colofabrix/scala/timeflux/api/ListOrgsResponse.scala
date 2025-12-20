package com.colofabrix.scala.timeflux.api

import io.circe.*
import io.circe.derivation.*
import java.time.OffsetDateTime

/**
 * List Organizations Response
 */
final case class ListOrgsResponse(
  orgs: List[Organization],
  links: Option[Links],
) derives Decoder

/**
 * Organization data model
 */
final case class Organization(
  id: String,
  name: String,
  description: Option[String],
  createdAt: Option[OffsetDateTime],
  updatedAt: Option[OffsetDateTime],
  links: Option[Links],
) derives Decoder
