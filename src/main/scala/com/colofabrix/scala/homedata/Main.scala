package com.colofabrix.scala.homedata

import cats.effect.*
import cats.implicits.given
import ch.qos.logback.classic.{ Level, LoggerContext }
import com.colofabrix.scala.declinio.*
import com.colofabrix.scala.homedata.influx.*
import com.colofabrix.scala.homedata.octopus.*
import com.colofabrix.scala.homedata.scrape.*
import com.colofabrix.scala.homedata.tado.*
import com.colofabrix.scala.homedata.utils.TimeSpanPicker
import com.colofabrix.scala.timeflux.*
import java.time.temporal.ChronoUnit
import org.slf4j.LoggerFactory
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import scala.jdk.CollectionConverters._

object Main extends IOUnitDeclineApp {

  implicit private val logger: Logger[IO] =
    Slf4jLogger.getLogger[IO]

  override def name: String =
    "HomeData"

  override def header: String =
    "Tado and Octopus scrapers"

  override def runNoConfig: IO[ExitCode] =
    configureLogging() >>
    loop.foreverM

  private def loop: IO[ExitCode] =
    val (from, to) =
      TimeSpanPicker()
        .selectFrom()
        .setDate(2025, 11, 23)
        .selectTo()
        .now()
        .roundBoth(ChronoUnit.DAYS)
        .pick()

    ScrapeLog[IO](HomedataConfig.config.scrapeLog.logPath)
      .use { scrapeLog =>
        for
          _                  <- IO.println("\nHomeData - Tado and Octopus scrapers\n")
          octoPuller         <- OctopusPuller(scrapeLog)
          gasMeasures         = octoPuller.pullGasReadings(from, to)
          electricityMeasures = octoPuller.pullElectricityReadings(from, to)
          tadoPuller         <- TadoPuller(scrapeLog)
          tadoMeasures        = tadoPuller.pullReadings(from, to)
          writer             <- InfluxWriter()
          _                  <- writer.write(tadoMeasures, gasMeasures, electricityMeasures)
          _                  <- electricityMeasures.compile.drain
          _                  <- gasMeasures.compile.drain
          _                  <- IO.println("\nHomeData - Scraping complited")
          _                  <- IO.println(s"Sleeping ${HomedataConfig.config.pollTime}...")
          _                  <- IO.sleep(HomedataConfig.config.pollTime)
        yield ExitCode.Success
      }

  private def configureLogging(): IO[Unit] =
    val context = LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext]
    System
      .getProperties
      .asScala
      .toList
      .traverse { (k, v) =>
        if k.toString.startsWith("logging.") then
          val loggerName = k.toString.stripPrefix("logging.")
          val level      = Level.toLevel(v.toString, Level.DEBUG)
          context.getLogger(loggerName).setLevel(level)
          logger.info(s"Setting custom log level: $loggerName -> $level")
        else
          IO.unit
      }
      .as(())

}
