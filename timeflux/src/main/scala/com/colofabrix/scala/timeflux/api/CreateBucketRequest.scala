package com.colofabrix.scala.timeflux.api

import io.circe.*
import io.circe.derivation.*
import com.colofabrix.scala.timeflux.model.OrgIdHandler

/**
 * Create Bucket POST Request
 */
final case class CreateBucketRequest(
  name: String,
  orgID: Option[String],
  description: Option[String],
  retentionRules: List[RetentionRules],
) derives Encoder.AsObject

object CreateBucketRequest:

  given OrgIdHandler[CreateBucketRequest] with

    def get(value: CreateBucketRequest): Option[String] =
      value.orgID

    def set(value: CreateBucketRequest, orgID: Option[String]): CreateBucketRequest =
      value.copy(orgID = orgID )
