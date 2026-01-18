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
import java.time.temporal.ChronoUnit

object Main extends IOUnitDeclineApp {

  override def name: String =
    "HomeData"

  override def header: String =
    "Tado and Octopus scrapers"

  import scala.concurrent.duration.*
  override def runNoConfig: IO[ExitCode] = {
    // loop >> IO.sleep(InfluxConfig.config.timeResolution)
    loop >> IO.sleep(5.seconds)
  }.foreverM

  private def loop: IO[ExitCode] =
    val (from, to) =
      TimeSpanPicker()
        .selectFrom()
        .setDate(2025, 11, 23)
        .selectTo()
        .now()
        .roundBoth(ChronoUnit.DAYS)
        .pick()

    ScrapeLog[IO](HomedataConfig.config.scrapeLog.logPath).use { scrapeLog =>
      for
        _                  <- IO.println("\nHomeData - Tado and Octopus scrapers\n")
        octoPuller         <- OctopusPuller(scrapeLog)
        gasMeasures         = octoPuller.pullGasReadings(from, to)
        electricityMeasures = octoPuller.pullElectricityReadings(from, to)
        tadoPuller         <- TadoPuller(scrapeLog)
        tadoMeasures        = tadoPuller.pullReadings(from, to)
        writer             <- InfluxWriter()
        _                  <- writer.write(tadoMeasures, gasMeasures, electricityMeasures)
        _                  <- IO.println("\nHomeData - SCRAPING COMPLETED\n")
      yield ExitCode.Success
    }

}
