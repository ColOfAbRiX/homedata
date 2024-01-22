package reflux

import com.github.plokhotnyuk.jsoniter_scala.macros.*
import com.github.plokhotnyuk.jsoniter_scala.core.*

object api:

  given influxRequestCodec: JsonValueCodec[InfluxRequest] =
    JsonCodecMaker.make

  given influxResponseCodec: JsonValueCodec[InfluxResponse] =
    JsonCodecMaker.make

  //  Requests  //

  enum InfluxRequest:

    case CreateBucketRequest[R <: InfluxResponse](
      description: Option[String],
      name: String,
      orgID: String,
      retentionRules: Seq[RetentionRules],
      rp: Option[String],
      schemaType: Option[String],
    ) extends InfluxRequest

  //  Responses  //

  enum InfluxResponse:

    case CreateBucketResponse(
      createdAt: String,
      description: String,
      id: String,
      labels: Seq[Labels],
      links: Links,
      name: String,
      orgID: String,
      retentionRules: Seq[RetentionRules],
      schemaType: String,
      `type`: String,
      updatedAt: String,
    ) extends InfluxResponse

  //  Others  //

  final case class RetentionRules(
    everySeconds: Int,
    shardGroupDurationSeconds: Int,
    `type`: String,
  )

  final case class Links(
    labels: String,
    members: String,
    org: String,
    owners: String,
    self: String,
    write: String,
  )

  final case class Labels(
    id: String,
    name: String,
    orgID: String,
    properties: Map[String, String],
  )

  //  Newtypes  //

  opaque type InfluxAuthToken =
    String

  object InfluxAuthToken:
    extension (self: InfluxAuthToken) def value: String = self
    def apply(value: String): InfluxAuthToken =
      value

  opaque type OrganizationId =
    String

  object OrganizationId:
    extension (self: OrganizationId) def value: String = self
    def apply(value: String): OrganizationId =
      value
