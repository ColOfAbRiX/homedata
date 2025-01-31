package com.colofabrix.scala.timeflux.api

import com.colofabrix.scala.timeflux.encoding.UrlParamsEncoder
import com.colofabrix.scala.timeflux.model.OrgIdHandler
import io.circe.*
import io.circe.derivation.*

/**
 * Query Request
 */
final case class QueryRequest(
  query: String,
  orgID: Option[String],
)

object QueryRequest:

  final private[timeflux] case class QueryGetRequest(
    orgID: Option[String],
  ) derives UrlParamsEncoder

  final private[timeflux] case class QueryPostRequest(
    query: String,
  ) derives Encoder.AsObject

  given OrgIdHandler[QueryRequest] with

    def get(value: QueryRequest): Option[String] =
      value.orgID

    def set(value: QueryRequest, orgID: Option[String]): QueryRequest =
      value.copy(orgID = orgID )
