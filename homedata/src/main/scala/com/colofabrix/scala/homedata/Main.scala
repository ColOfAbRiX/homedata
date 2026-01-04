package com.colofabrix.scala.homedata

import cats.effect.*
import cats.implicits.given
import com.colofabrix.scala.declinio.*
import com.colofabrix.scala.homedata.influx.*
import com.colofabrix.scala.homedata.octopus.*
import com.colofabrix.scala.homedata.scrape.*
import com.colofabrix.scala.homedata.tado.*
import com.colofabrix.scala.homedata.utils.TimeSpanPicker
import com.colofabrix.scala.timeflux.*
import com.colofabrix.scala.timeflux.measures.TimefluxSerializable.toApiMeasureStream
import java.time.temporal.ChronoUnit

object Main extends IOUnitDeclineApp {

  override def name: String =
    "HomeData"

  override def header: String =
    "Tado and Octopus scrapers"

  override def runNoConfig: IO[ExitCode] =
    val (from, to) =
      TimeSpanPicker()
        .selectFrom()
        .setDate(2025, 11, 23)
        .selectTo()
        .now()
        .roundBoth(ChronoUnit.DAYS)
        .pick()

    val query =
      """from(bucket: "home_data")
        |  |> range(start: -3d)
        |  |> filter(fn: (r) => r["_measurement"] == "electricity")
        |  |> filter(fn: (r) => r["_field"] == "consumption")
        |  |> aggregateWindow(every: 1h, fn: mean, createEmpty: false)
        |  |> yield(name: "mean")""".stripMargin

    ScrapeLog[IO](HomedataConfig.config.scrapeLog.logPath).use { scrapeLog =>
      for
        _                  <- IO.println("\nHomeData - Tado and Octopus scrapers\n")
        octoPuller         <- OctopusPuller(scrapeLog)
        gasMeasures         = octoPuller.pullGasReadings(from, to).through(toApiMeasureStream)
        electricityMeasures = octoPuller.pullElectricityReadings(from, to).through(toApiMeasureStream)
        tadoPuller         <- TadoPuller(scrapeLog)
        tadoMeasures        = tadoPuller.pullReadings(from, to).through(toApiMeasureStream)
        writer             <- InfluxWriter()
        _                  <- writer.write(tadoMeasures, gasMeasures, electricityMeasures)
        _                  <- IO.println("\nHomeData - Simple statistics\n")
        timefluxClient     <- InfluxReader()
        points             <- timefluxClient.query(query)
        listPoints         <- points.compile.toList
        _                  <- listPoints.traverse(IO.println)
      yield ExitCode.Success
    }

}
