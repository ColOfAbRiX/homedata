package com.colofabrix.scala.timeflux.api

import com.colofabrix.scala.timeflux.encoding.UrlParamsEncoder
import com.colofabrix.scala.timeflux.model.OrgIdHandler

/**
 * List Bucket POST Request
 */
final case class ListBucketRequest(
  name: Option[String],
) derives UrlParamsEncoder

object ListBucketRequest:

  given OrgIdHandler[ListBucketRequest] with

    def get(value: ListBucketRequest): Option[String] =
      None

    def set(value: ListBucketRequest, orgID: Option[String]): ListBucketRequest =
      value
