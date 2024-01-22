package reflux.api

import io.circe.syntax.*
import io.circe.*
import io.circe.derivation.*
import java.time.Instant

//  Requests  //

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
  ) extends InfluxRequest

//  Responses  //

sealed trait InfluxResponse

object InfluxResponse:

  final case class CreateBucket(
    name: String,
    retentionRules: List[RetentionRules],
    createdAt: Option[Instant],
    description: Option[String],
    id: Option[String],
    labels: Option[List[Labels]],
    links: Option[Links],
    orgID: Option[String],
    schemaType: Option[String],
    `type`: Option[String],
    updatedAt: Option[Instant],
  ) extends InfluxResponse derives Codec.AsObject

  final case class ListBuckets(
    buckets: List[Buckets],
    links: Option[Links],
  ) extends InfluxResponse derives Codec.AsObject

//  Others  //

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
