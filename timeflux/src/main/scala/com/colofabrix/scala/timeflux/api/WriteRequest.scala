package com.colofabrix.scala.timeflux.api

import com.colofabrix.scala.timeflux.encoding.UrlParamsEncoder

/**
 * Write GET Request
 */
final case class WriteRequest(
  bucket: String,
  precision: String,
) derives UrlParamsEncoder

