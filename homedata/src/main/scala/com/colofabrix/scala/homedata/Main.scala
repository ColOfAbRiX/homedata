package com.colofabrix.scala.homedata

import cats.effect.*
import com.colofabrix.scala.homedata.influx.InfluxDbConfig
import com.colofabrix.scala.homedata.influx.InfluxDbConfig.{ config => InfluxConf }
import com.colofabrix.scala.homedata.octopus.Octopus
import com.colofabrix.scala.homedata.tado.*
import com.colofabrix.scala.timeflux.*
import com.colofabrix.scala.timeflux.measures.Measure
import com.colofabrix.scala.timeflux.measures.TimefluxSerializable
import fs2.concurrent.Channel
import java.time.*
import java.time.temporal.ChronoUnit

object Main extends IOApp.Simple with TimefluxDSL:

  val periodFrom = OffsetDateTime.now.minus(1, ChronoUnit.MONTHS)
  val periodTo   = OffsetDateTime.now
  // val periodFrom = OffsetDateTime.of(2024, 12, 13, 1, 0, 0, 0, ZoneOffset.UTC)
  // val periodTo   = OffsetDateTime.of(2024, 12, 15, 1, 0, 0, 0, ZoneOffset.UTC)

  val run =
    for {
      timefluxClient <- TimefluxClient[IO](InfluxDbConfig.clientConfig)
      _              <- timefluxClient.createBucketIfMissing(InfluxConf.projectBucket)
      channel        <- Channel.unbounded[IO, Measure]
      tadoPuller     <- TadoPuller()
      tadoReading     = tadoPuller.pullReadings(periodFrom, periodTo)
      tadoMeasures    = tadoReading.through(TimefluxSerializable.toApiMeasureStream)
      octopusReadings = Octopus.pullReadings(periodFrom)
      octopusMeasures = octopusReadings.through(TimefluxSerializable.toApiMeasureStream)
      allMeasures     = tadoMeasures merge octopusMeasures
      _              <- timefluxClient.writeMeasures(InfluxConf.projectBucket, allMeasures)
    } yield ()
