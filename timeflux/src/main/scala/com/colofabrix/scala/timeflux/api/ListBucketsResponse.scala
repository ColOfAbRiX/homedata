package com.colofabrix.scala.timeflux.api

import io.circe.*
import io.circe.derivation.*

/**
 * List Buckets Response
 */
final case class ListBucketsResponse(
  buckets: List[Buckets],
  links: Option[Links],
) derives Decoder
