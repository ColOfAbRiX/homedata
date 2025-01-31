package com.colofabrix.scala.timeflux.api

import com.colofabrix.scala.timeflux.encoding.UrlParamsEncoder
import com.colofabrix.scala.timeflux.model.OrgIdHandler

/**
 * Delete Bucket DEL Request
 */
final case class DeleteBucketRequest(
  bucketID: String,
) derives UrlParamsEncoder

object DeleteBucketRequest:

  given OrgIdHandler[DeleteBucketRequest] with

    def get(value: DeleteBucketRequest): Option[String] =
      None

    def set(value: DeleteBucketRequest, orgID: Option[String]): DeleteBucketRequest =
      value
