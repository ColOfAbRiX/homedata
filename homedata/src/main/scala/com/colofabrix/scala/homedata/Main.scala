package com.colofabrix.scala.homedata

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
      _              <- timefluxClient.createBucketIfMissing(InfluxDbConfig.config.octopusBucket)
      // octopus  = octpusReadings(periodFrom) merge tadoMeasurements(periodFrom)
      tado = tadoMeasurements(periodFrom, periodTo)
      // _         = tado.foreach(println)
      // allReadings <- octopus merge tado
      result <- timefluxClient.write(InfluxDbConfig.config.octopusBucket, "s", tado)
    } yield ()

  private def tadoMeasurements(from: OffsetDateTime, to: OffsetDateTime): fs2.Stream[IO, Measure] =
    for
      tado    <- fs2.Stream.eval(Tado())
      reading <- tado.pullReadings(from, to)
    yield reading.toMeasure

  private def octopusMeasurements(from: OffsetDateTime): fs2.Stream[IO, Measure] =
    Octopus
      .pullReadings(from)
      .map(_.toMeasure)
