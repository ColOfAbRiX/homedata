package com.colofabrix.scala.homedata

import cats.implicits.given
import cats.effect.*
import com.colofabrix.scala.homedata.influx.InfluxDbConfig
import com.colofabrix.scala.homedata.octopus.*
import com.colofabrix.scala.homedata.tado.*
import com.colofabrix.scala.timeflux.*
import com.colofabrix.scala.timeflux.api.*
import com.colofabrix.scala.timeflux.config.TimefluxClientConfig
import com.colofabrix.scala.timeflux.measures.*
import java.time.*
import java.time.temporal.ChronoUnit

object Main extends IOApp.Simple with TimefluxDSL:

  val periodFrom = OffsetDateTime.now.minus(2, ChronoUnit.DAYS)
  val periodTo   = OffsetDateTime.now

  val run =
    for {
      timefluxClient <- TimefluxClient[IO](InfluxDbConfig.clientConfig)
      result              <- timefluxClient.createBucketIfMissing(InfluxDbConfig.config.projectBucket)
      tadoPuller     <- TadoPuller()
      tadoReading     = tadoPuller.pullReadings(periodFrom, periodTo)
      // octopus  = octpusReadings(periodFrom) merge tadoMeasurements(periodFrom)
      // allReadings <- octopus merge tado
      result <- timefluxClient.write(InfluxDbConfig.config.projectBucket, tadoReading.take(3))
    } yield ()

  private def octopusMeasurements(from: OffsetDateTime): fs2.Stream[IO, Measure] =
    Octopus
      .pullReadings(from)
      .map(_.toMeasure)
