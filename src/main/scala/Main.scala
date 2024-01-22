package com.colofabrix.scala.homedata

import cats.effect.*
import cats.effect.implicits.*
import cats.implicits.*
import java.time.*
import java.time.temporal.ChronoUnit
import reflux.*
import org.http4s.Uri
import reflux.api.*
import reflux.config.*
import fs2.Stream
import reflux.Reflux.client

final case class ElectricityReading(time: Instant, value: Double)

object Main extends IOApp.Simple:

  val run =
    val pageSize      = 100
    val periodTo      = Instant.now()
    val periodFrom    = periodTo.minus(3, ChronoUnit.DAYS)
    val octopusBucket = "octopus_electricity"

    Stream
      .unfoldChunkEval(1) { page =>
        for
          readings <- Electricity.readConsumption(Some(periodFrom), Some(periodTo), page, pageSize)
          result    = if readings.size == 0 then None else Some((readings, page + 1))
        yield result
      }
      .map { reading =>
        println(reading)
      }
      .compile
      .drain

  private val refluxClient =
    Reflux
      .clientIO(
        Uri.unsafeFromString("http://127.0.0.1:8086"),
        OrganizationId("dd87acad77d7e016"),
        InfluxAuthToken("VRB6CbkkF4CcAG8dKHu56D_t8kA4IumEMI5-QazdKz0ry3vArEe3tOCSw3YvgTnHinIEicUXpEC-5rI3Zu1rjQ=="),
      )

  private def haveBucket(client: InfluxClient[IO], bucket: String): IO[Unit] =
    for
      found  <- client.listBuckets(Some(bucket))
      result <- if found.buckets.isEmpty then client.createBucket(bucket).void else IO.unit
      _      <- IO(println(s"Bucket $bucket is there"))
    yield result
