// package com.colofabrix.scala.homedata.octopus

// import cats.effect.IO
// import fs2.Chunk
// import java.time.*
// import org.json4s.*
// import org.json4s.native.JsonMethods.*
// import sttp.client4.*
// import sttp.client4.httpclient.HttpClientSyncBackend
// import scala.annotation.nowarn

// object OctopusGas:

//   def pullPage(fromDate: OffsetDateTime)(pageNumber: Int): IO[(Chunk[GasReading], Boolean)] =
//     pull(Some(fromDate), None, pageNumber, OctopusConfig.PageSize)

//   def pull(
//     fromDate: Option[OffsetDateTime],
//     toDate: Option[OffsetDateTime],
//     pageNumber: Int,
//     pageSize: Int,
//   ): IO[(Chunk[GasReading], Boolean)] =
//     val endpointUrl =
//       OctopusConfig.GasConsumptionUrl
//         .addParam("period_from", fromDate.map(_.toString))
//         .addParam("period_to", toDate.map(_.toString))
//         .addParam("page", pageNumber.toString)
//         .addParam("page_size", pageSize.toString)

//     IO {
//       basicRequest
//         .get(endpointUrl)
//         .auth
//         .basic(user = OctopusConfig.config.apiKey, password = "")
//         .send(HttpClientSyncBackend())
//         .body
//         .toOption
//         .map(deserializeResponse)
//         .getOrElse {
//           (Chunk.empty, false)
//         }
//     }

//   @nowarn
//   private def deserializeResponse(body: String): (Chunk[GasReading], Boolean) =
//     println("GAS")
//     println(body)
//     throw new RuntimeException("")
//     val json = parse(body)

//     val hasNext =
//       json \ "next" match
//         case JNull => false
//         case _     => true

//     val readings =
//       for
//         case JArray(results) <- json \ "results"
//         case JObject(result) <- results
//         case JField("consumption", JDouble(value)) <- result
//         case JField("interval_start", JString(time)) <- result
//         intervalStart = OffsetDateTime.parse(time)
//         reading       = GasReading(intervalStart, value)
//       yield reading

//     (Chunk.from(readings), hasNext)
