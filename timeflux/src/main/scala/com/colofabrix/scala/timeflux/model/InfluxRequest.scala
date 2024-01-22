package com.colofabrix.scala.timeflux.model

import io.circe.*
import io.circe.derivation.*
import com.colofabrix.scala.restbee.encoding.UrlParamsEncoder

transparent trait InfluxRequest

final case class CreateBucketRequest(
  name: String,
  orgID: String,
  description: Option[String],
  retentionRules: List[RetentionRules],
) extends InfluxRequest derives Codec.AsObject

final case class ListBucketRequest(
  name: Option[String],
) extends InfluxRequest derives UrlParamsEncoder

final case class WriteRequest(
  bucket: String,
  orgID: String,
  precision: String,
) extends InfluxRequest derives UrlParamsEncoder
