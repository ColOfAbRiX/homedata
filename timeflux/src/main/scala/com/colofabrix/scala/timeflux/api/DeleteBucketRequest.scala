package com.colofabrix.scala.timeflux.api

import com.colofabrix.scala.timeflux.encoding.UrlParamsEncoder

/**
 * Delete Bucket DEL Request
 */
final case class DeleteBucketRequest(
  bucketID: String,
) derives UrlParamsEncoder
