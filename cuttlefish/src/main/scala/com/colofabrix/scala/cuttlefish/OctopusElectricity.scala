// package com.colofabrix.scala.homedata.octopus

// import cats.effect.IO
// import fs2.Chunk
// import java.time.*
// import org.json4s.*
// import org.json4s.native.JsonMethods.*
// import sttp.client4.*
// import sttp.client4.httpclient.HttpClientSyncBackend

// object OctopusElectricity:

//   def pull(
//     fromDate: Option[OffsetDateTime],
//     toDate: Option[OffsetDateTime],
//     pageNumber: Int,
//     pageSize: Int,
//   ): IO[(Chunk[ElectricityReading], Boolean)] =
//     val endpointUrl =
//       OctopusConfig.ElectricityConsumptionUrl
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

//   private def deserializeResponse(body: String): (Chunk[ElectricityReading], Boolean) =
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
//         reading       = ElectricityReading(intervalStart, value)
//       yield reading

//     (Chunk.from(readings), hasNext)
