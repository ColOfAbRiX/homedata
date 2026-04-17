package com.colofabrix.scala.homedata

import cats.effect.*
import cats.syntax.all.*
import ch.qos.logback.classic.{ Level, LoggerContext }
import com.colofabrix.scala.declinio.*
import com.colofabrix.scala.homedata.influx.*
import com.colofabrix.scala.homedata.octopus.*
import com.colofabrix.scala.homedata.scrape.*
import com.colofabrix.scala.homedata.tado.*
import com.colofabrix.scala.homedata.utils.*
import com.colofabrix.scala.homedata.utils.TimeSpanPicker
import com.colofabrix.scala.timeflux.*
import java.time.OffsetDateTime
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
    loop()

  private def loop(): IO[ExitCode] =
    runScraping()
      .flatMap { result =>
        HomedataConfig.config.pollTime match {
          case Some(pollTime) if result != ExitCode.Success =>
            result.pure
          case Some(pollTime) =>
            s"Sleeping $pollTime...".stdout >>
            pollTime.sleep >>
            loop()
          case None =>
            result.pure
        }
      }

  private def runScraping(): IO[ExitCode] =
    val from = HomedataConfig.config.scrapeFromDate
    val to   = HomedataConfig.config.scrapeToDate.getOrElse(OffsetDateTime.now())

    ScrapeLog[IO](HomedataConfig.config.scrapeLog.logPath)
      .use { scrapeLog =>
        for
          _ <- "\nHomeData - Tado and Octopus scrapers\n".stdout
          octoPuller         <- OctopusPuller(scrapeLog)
          gasMeasures         = octoPuller.pullGasReadings(from, to)
          electricityMeasures = octoPuller.pullElectricityReadings(from, to)
          tadoPuller         <- TadoPuller(scrapeLog)
          tadoMeasures        = tadoPuller.pullReadings(from, to)
          writer             <- InfluxWriter()
          _                  <- writer.write(tadoMeasures, gasMeasures, electricityMeasures)
          _                  <- electricityMeasures.compile.drain
          _                  <- gasMeasures.compile.drain
          _ <- "\nHomeData - Scraping completed".stdout
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
