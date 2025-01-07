package com.colofabrix.scala.homedata

import cats.effect.*
import com.colofabrix.scala.homedata.influx.InfluxDbConfig
import com.colofabrix.scala.homedata.octopus.*
import com.colofabrix.scala.homedata.tado.*
import com.colofabrix.scala.timeflux.*
import com.colofabrix.scala.timeflux.measures.TimefluxSerializable
import java.time.*

object Main extends IOApp.Simple with TimefluxDSL:

  val from = OffsetDateTime.of(2025, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
  val to = OffsetDateTime.of(2025, 1, 2, 0, 0, 0, 0, ZoneOffset.UTC)
  //val to   = OffsetDateTime.now

  val run =
    for {
      // Octopus
      octoPuller    <- OctopusPuller()
      gasReadings    = octoPuller.pullGasReadings(from, to)
      electrReadings = octoPuller.pullElectricityReadings(from, to)
      octoReadings   = gasReadings merge electrReadings
      octoMeasures   = octoReadings.through(TimefluxSerializable.toApiMeasureStream)
      // Tado
      tadoPuller  <- TadoPuller()
      tadoReadings = tadoPuller.pullReadings(from, to)
      tadoMeasures = tadoReadings.through(TimefluxSerializable.toApiMeasureStream)
      // Timeflux
      allMeasures     = tadoMeasures merge octoMeasures
      _              <- allMeasures.evalTap(IO.println).compile.drain
      timefluxClient <- TimefluxClient[IO](InfluxDbConfig.clientConfig)
      // _              <- timefluxClient.createBucketIfMissing(InfluxDbConfig.config.projectBucket)
      // _ <- timefluxClient.writeMeasures(
      //        InfluxDbConfig.config.projectBucket,
      //        allMeasures,
      //        batchWrites = Some(InfluxDbConfig.config.batchWrites),
      //      )
    } yield ()
