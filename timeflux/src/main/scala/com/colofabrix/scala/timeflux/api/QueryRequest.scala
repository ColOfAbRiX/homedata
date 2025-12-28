package com.colofabrix.scala.timeflux.api

import com.colofabrix.scala.timeflux.encoding.UrlParamsEncoder
import io.circe.*
import io.circe.derivation.*

/**
 * Query Request
 */
final case class QueryRequest(
  query: String,
  orgID: Option[String],
)

object QueryRequest {

  final private[timeflux] case class QueryGetRequest(
    orgID: Option[String],
  ) derives UrlParamsEncoder

  final private[timeflux] case class QueryPostRequest(
    query: String,
  ) derives Encoder.AsObject

}
