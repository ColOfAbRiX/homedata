package com.colofabrix.scala.homedata

import fs2.Chunk
import java.time.*
import sttp.client4.*
import org.json4s.*
import org.json4s.native.JsonMethods.*
import org.json4s.native.Serialization
import org.json4s.native.Serialization.read

object Electricity:

  case class ElectricityReading(time: Instant, value: Double)

  def readConsumption(
      fromDate: Option[Instant],
      toDate: Option[Instant],
      page: Int,
      pageSize: Int
  ): Chunk[ElectricityReading] =
    val endpointUrl =
      Octopus.ElectricityConsumptionUrl
        .addParam("period_from", fromDate.map(_.toString))
        .addParam("period_to", toDate.map(_.toString))
        .addParam("page", page.toString)
        .addParam("page_size", pageSize.toString)

    val response =
      basicRequest
        .get(endpointUrl)
        .auth
        .basic(user = Octopus.ApiKey, password = "")
        .send(Backend.backend)

    response.body.toOption
      .map(deserializeBody)
      .map(Chunk.from)
      .getOrElse {
        println(s"ERROR: $response")
        Chunk.empty
      }

  private def deserializeBody(body: String): List[ElectricityReading] =
    given formats: Formats = Serialization.formats(NoTypeHints)
    for
      case JArray(results) <- parse(body) \ "results"
      case JObject(result) <- results
      case JField("consumption", JDouble(value)) <- result
      case JField("interval_start", JString(time)) <- result
      intervalStart = Instant.parse(time)
      result        = ElectricityReading(intervalStart, value)
    yield result

  private def deserializeBody2(body: String): List[ElectricityReading] =
    val json = parse(body)
    val results = json \ "results"

    ???
