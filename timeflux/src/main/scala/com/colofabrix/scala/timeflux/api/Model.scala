package com.colofabrix.scala.timeflux.api

import io.circe.*
import io.circe.derivation.*

final case class Buckets(
  name: String,
  retentionRules: List[RetentionRules],
  createdAt: Option[String],
  description: Option[String],
  id: Option[String],
  labels: Option[List[Labels]],
  links: Option[Links],
  orgID: Option[String],
  schemaType: Option[String],
  `type`: Option[String],
  updatedAt: Option[String],
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
