package com.colofabrix.scala.homedata

import cats.effect.*
import com.colofabrix.scala.homedata.influx.InfluxDbConfig
import com.colofabrix.scala.homedata.influx.InfluxDbConfig.{ config => InfluxConf }
import com.colofabrix.scala.homedata.octopus.Octopus
import com.colofabrix.scala.homedata.tado.*
import com.colofabrix.scala.timeflux.*
import com.colofabrix.scala.timeflux.measures.TimefluxSerializable
import java.time.*

object Main extends IOApp.Simple with TimefluxDSL:

  val periodFrom = OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
  val periodTo = OffsetDateTime.now

  val run =
    for {
      // Tado
      tadoPuller  <- TadoPuller()
      tadoReading  = tadoPuller.pullReadings(periodFrom, periodTo)
      tadoMeasures = tadoReading.through(TimefluxSerializable.toApiMeasureStream)
      // Octopus
      octopusReadings = Octopus.pullReadings(periodFrom, periodTo)
      octopusMeasures = octopusReadings.through(TimefluxSerializable.toApiMeasureStream)
      // Timeflux
      timefluxClient <- TimefluxClient[IO](InfluxDbConfig.clientConfig)
      _              <- timefluxClient.createBucketIfMissing(InfluxConf.projectBucket)
      allMeasures     = tadoMeasures merge octopusMeasures
      _ <- timefluxClient.writeMeasures(
             InfluxConf.projectBucket,
             allMeasures,
             batchWrites = Some(InfluxConf.batchWrites),
           )
    } yield ()
