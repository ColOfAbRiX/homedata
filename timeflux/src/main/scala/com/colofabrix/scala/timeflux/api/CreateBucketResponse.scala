package com.colofabrix.scala.timeflux.api

import io.circe.*
import io.circe.derivation.*
import java.time.Instant

/**
 * Create Bucket Response
 */
final case class CreateBucketResponse(
  name: String,
  retentionRules: List[RetentionRules],
  createdAt: Option[Instant],
  description: Option[String],
  id: Option[String],
  labels: Option[List[Labels]],
  links: Option[Links],
  orgID: Option[String],
  schemaType: Option[String],
  `type`: Option[String],
  updatedAt: Option[Instant],
) derives Decoder
