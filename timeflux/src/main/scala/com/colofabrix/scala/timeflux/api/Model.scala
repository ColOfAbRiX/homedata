package com.colofabrix.scala.timeflux.api

import cats.kernel.Order
import com.colofabrix.scala.timeflux.encoding.UrlParamsEncoder
import io.circe.*
import io.circe.derivation.*
import java.time.*

enum TimePrecision(val value: String, val multiplier: Long) {

  case Seconds      extends TimePrecision("s", 0)
  case Milliseconds extends TimePrecision("ms", 3)
  case Microseconds extends TimePrecision("us", 6)
  case Nanoseconds  extends TimePrecision("ns", 9)

}

object TimePrecision {

  given Order[TimePrecision] with
    def compare(x: TimePrecision, y: TimePrecision): Int =
      (x.multiplier - y.multiplier).toInt

  given UrlParamsEncoder[TimePrecision] with
    def encode(a: TimePrecision): Map[String, String] =
      Map("" -> a.value)

}

final case class Buckets(
  name: String,
  retentionRules: List[RetentionRules],
  createdAt: Option[OffsetDateTime],
  description: Option[String],
  id: Option[String],
  labels: Option[List[Labels]],
  links: Option[Links],
  orgID: Option[String],
  schemaType: Option[String],
  `type`: Option[String],
  updatedAt: Option[OffsetDateTime],
) derives Codec.AsObject

final case class RetentionRules(
  everySeconds: Int,
  shardGroupDurationSeconds: Option[Int],
  `type`: Option[String],
) derives Codec.AsObject

final case class Links(
  labels: Option[String],
  members: Option[String],
  org: Option[String],
  owners: Option[String],
  self: Option[String],
  write: Option[String],
) derives Codec.AsObject

final case class Labels(
  id: Option[String],
  name: Option[String],
  orgID: Option[String],
  properties: Map[String, String],
) derives Codec.AsObject
