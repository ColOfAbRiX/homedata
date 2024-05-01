package com.colofabrix.scala.timeflux.api

import com.colofabrix.scala.timeflux.encoding.UrlParamsEncoder

/**
 * List Bucket POST Request
 */
final case class ListBucketRequest(
  name: Option[String],
) derives UrlParamsEncoder
