package com.colofabrix.scala.timeflux.api

import com.colofabrix.scala.timeflux.encoding.UrlParamsEncoder
import com.colofabrix.scala.timeflux.model.OrgIdHandler

/**
 * Write GET Request
 */
final case class WriteRequest(
  bucket: String,
  orgID: Option[String],
  precision: TimePrecision,
  batchWrites: Option[Int]
) derives UrlParamsEncoder

object WriteRequest:

  given OrgIdHandler[WriteRequest] with

    def get(value: WriteRequest): Option[String] =
      value.orgID

    def set(value: WriteRequest, orgID: Option[String]): WriteRequest =
      value.copy(orgID = orgID )
