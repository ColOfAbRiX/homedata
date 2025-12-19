package com.colofabrix.scala.homedata

import cats.effect.*
import cats.implicits.given
import com.colofabrix.scala.declinio.*
import com.colofabrix.scala.homedata.influx.*
import com.colofabrix.scala.homedata.octopus.*
import com.colofabrix.scala.homedata.tado.*
import com.colofabrix.scala.timeflux.*
import com.colofabrix.scala.timeflux.api.QueryRequest
import com.colofabrix.scala.timeflux.measures.TimefluxSerializable.toApiMeasureStream
import com.monovore.decline.Opts
import java.time.*
import java.time.temporal.ChronoUnit

object Main extends IODeclineApp[Unit]:

  override def name: String =
    "HomeData"

  override def header: String =
    "Tado and Octopus scrapers"

  override def options: Opts[Unit] =
    Opts.unit

  override def runWithConfig(config: Unit): IO[ExitCode] =
    val (from, to) =
      TimeSpanPicker()
        .selectFrom()
        .otherMinus(days = 3)
        .roundBoth(ChronoUnit.DAYS)
        .pick()

    val query =
      """from(bucket: "home_data")
        ||>  range(start: -1d)
        ||>  filter(fn: (r) => r["_measurement"] == "gas")
        ||>  filter(fn: (r) => r["_field"] == "consumption")
        ||>  aggregateWindow(every: 1h, fn: mean, createEmpty: false)
        ||>  yield(name: "mean")""".stripMargin

    for
      // Real working Tado/Octopus
      _ <- IO.println("HomeData - Tado and Octopus scrapers")
      octoPuller         <- OctopusPuller()
      gasMeasures         = octoPuller.pullGasReadings(from, to).through(toApiMeasureStream)
      electricityMeasures = octoPuller.pullElectricityReadings(from, to).through(toApiMeasureStream)
      tadoPuller         <- TadoPuller()
      tadoMeasures        = tadoPuller.pullReadings(from, to).through(toApiMeasureStream)
      writer             <- InfluxWriter()
      _                  <- writer.write(tadoMeasures, gasMeasures, electricityMeasures)
      // Extra testing code for influx syntax
      _ <- IO.println("HomeData - Simple statistics")
      timefluxClient <- TimefluxClient[IO](InfluxConfig.clientConfig)
      points         <- timefluxClient.query(QueryRequest(query, None))
      listPoints     <- points.compile.toList
      _              <- listPoints.traverse(IO.println)
    yield ExitCode.Success
