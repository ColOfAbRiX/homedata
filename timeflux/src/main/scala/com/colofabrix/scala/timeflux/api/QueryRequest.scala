package com.colofabrix.scala.timeflux.api

import com.colofabrix.scala.timeflux.encoding.UrlParamsEncoder

/** Query Request for Flux */
final case class QueryRequest(
  query: String,
  orgID: String,
)

object QueryRequest {

  final private[timeflux] case class QueryGetRequest(
    orgID: String,
  ) derives UrlParamsEncoder

}
