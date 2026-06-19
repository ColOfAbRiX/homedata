package com.colofabrix.scala.homedata

import cats.effect.*
import cats.syntax.all.*
import com.colofabrix.scala.cuttlefish.CuttlefishClient
import com.colofabrix.scala.timeflux.TimefluxClient
import com.colofabrix.scala.tado4s.Tado4sClient
import com.colofabrix.scala.homedata.influx.*
import com.colofabrix.scala.homedata.octopus.*
import com.colofabrix.scala.homedata.scrape.*
import com.colofabrix.scala.homedata.tado.*
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

final case class HomedataServices(
  scrapeLog: ScrapeLog[IO],
  cuttlefishClient: CuttlefishClient[IO],
  timefluxClient: TimefluxClient[IO],
  tadoClient: Tado4sClient[IO],
)

object HomedataServices {

  implicit private val logger: Logger[IO] =
    Slf4jLogger.getLogger[IO]

  def make(): Resource[IO, HomedataServices] =
    for {
      _                <- Resource.eval(logger.info("Initializing HomeData services..."))
      scrapeLog        <- ScrapeLog.make[IO](HomedataConfig.config.scrapeLog.logPath)
      _                <- Resource.eval(logger.debug("Creating CuttlefishClient..."))
      cuttlefishClient <- CuttlefishClient.make[IO]()
      _                <- Resource.eval(logger.debug("Creating TimefluxClient..."))
      timefluxClient   <- TimefluxClient.make[IO](InfluxConfig.clientConfig)
      _                <- Resource.eval(logger.debug("Creating Tado4sClient..."))
      tadoClient       <- Tado4sClient.make[IO]()
      resources         = HomedataServices(scrapeLog, cuttlefishClient, timefluxClient, tadoClient)
    } yield resources

}
