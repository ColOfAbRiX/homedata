package com.colofabrix.scala.homedata

import cats.effect.*
import com.colofabrix.scala.homedata.influx.*
import com.colofabrix.scala.homedata.octopus.*
import com.colofabrix.scala.homedata.tado.*
import com.colofabrix.scala.timeflux.*
import com.colofabrix.scala.timeflux.measures.TimefluxSerializable.toApiMeasureStream
import java.time.*

object Main extends IOApp.Simple with TimefluxDSL:

  val from = OffsetDateTime.of(2024, 10, 1, 0, 0, 0, 0, ZoneOffset.UTC)
  val to   = OffsetDateTime.of(2025, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
  // val to   = OffsetDateTime.now

  val run =
    for {
      octoPuller         <- OctopusPuller()
      gasMeasures         = octoPuller.pullGasReadings(from, to).through(toApiMeasureStream)
      electricityMeasures = octoPuller.pullElectricityReadings(from, to).through(toApiMeasureStream)
      tadoPuller         <- TadoPuller()
      tadoMeasures        = tadoPuller.pullReadings(from, to).through(toApiMeasureStream)
      // writer             <- TimefluxWriter()
      // _                  <- writer.write(gasMeasures, electricityMeasures, tadoMeasures)
      timefluxClient <- TimefluxClient[IO](InfluxDbConfig.clientConfig)
      _              <- timefluxClient.createBucketIfMissing(InfluxDbConfig.config.projectBucket)
      _ <- timefluxClient.writeMeasures(
        InfluxDbConfig.config.projectBucket,
        gasMeasures,
        batchWrites = Some(InfluxDbConfig.config.batchWrites),
      )
    } yield ()
