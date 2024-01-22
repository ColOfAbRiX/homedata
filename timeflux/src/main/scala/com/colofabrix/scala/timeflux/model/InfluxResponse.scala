package com.colofabrix.scala.timeflux.model

import io.circe.*
import io.circe.derivation.*
import java.time.Instant

transparent trait InfluxResponse

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
) extends InfluxResponse derives Codec.AsObject

/**
 * List Buckets Response
 */
final case class ListBucketsResponse(
  buckets: List[Buckets],
  links: Option[Links],
) extends InfluxResponse derives Codec.AsObject

/**
 * Influx Error Response
 */
final case class ErrorResponse(
  code: String,
  err: Option[String],
  message: Option[String],
  op: Option[String],
) derives Codec.AsObject
