package com.colofabrix.scala.timeflux.model

import io.circe.*
import io.circe.derivation.*
import com.colofabrix.scala.beerest.GetEncoder

transparent trait InfluxRequest

object InfluxRequest:

  final case class CreateBucket(
    name: String,
    orgID: String,
    description: Option[String],
    retentionRules: List[RetentionRules],
  ) extends InfluxRequest derives Codec.AsObject

  final case class ListBucket(
    name: Option[String],
  ) extends InfluxRequest derives GetEncoder
