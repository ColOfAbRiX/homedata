package com.colofabrix.scala.timeflux.api

import io.circe.*
import io.circe.derivation.*

/**
 * Create Bucket POST Request
 */
final case class CreateBucketRequest(
  name: String,
  orgID: String,
  description: Option[String],
  retentionRules: List[RetentionRules],
) derives Encoder.AsObject
