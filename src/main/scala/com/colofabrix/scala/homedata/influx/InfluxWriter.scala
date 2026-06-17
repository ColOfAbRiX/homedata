package com.colofabrix.scala.homedata.influx

import cats.effect.IO
import com.colofabrix.scala.homedata.models.DataEntry
import com.colofabrix.scala.homedata.scrape.ScrapeLog
import com.colofabrix.scala.homedata.utils.pipes.*
import com.colofabrix.scala.timeflux.*
import com.colofabrix.scala.timeflux.measures.Measure
import com.colofabrix.scala.timeflux.model.OrgId
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import java.time.OffsetDateTime
import scala.concurrent.duration.*

final class InfluxWriter(
  timefluxClient: TimefluxClient[IO],
  scrapeLog: ScrapeLog[IO],
  orgId: String,
) extends TimefluxDSL {

  implicit private val logger: Logger[IO] =
    Slf4jLogger.getLogger[IO]

  def write(values: fs2.Stream[IO, DataEntry]*): IO[Unit] =
    for
      _         <- logger.info("Wiring readings into database...")
      allStreams = values.foldLeft(fs2.Stream.empty[IO])(_ merge _)
      _         <- logger.info("Starting collection of data")
      result    <- write(allStreams)
    yield result

  def write(values: fs2.Stream[IO, DataEntry]): IO[Unit] =
    values
      .groupWithin(InfluxConfig.config.batchWrites, 100.millis)
      .evalMap { chunk =>
        logger.info(s"Submitting ${InfluxConfig.config.batchWrites} measures to InfluxDB") >>
        timefluxClient.writeMeasuresChunk(InfluxConfig.config.projectBucket, orgId, chunk.map(_.measure)) >>
        writeToScrapeLog(chunk)
      }
      .compile
      .drain

  private def writeToScrapeLog(chunk: fs2.Chunk[DataEntry]): IO[Unit] =
    fs2.Stream
      .chunk(chunk)
      .evalTap { dataEntry =>
        if dataEntry.timestamp.isBefore(OffsetDateTime.now()) then
          scrapeLog.logEntry(dataEntry.service, dataEntry.timestamp, dataEntry.entityId)
        else
          IO.unit
      }
      .compile
      .drain

}

object InfluxWriter extends TimefluxDSL {

  implicit private val logger: Logger[IO] =
    Slf4jLogger.getLogger[IO]

  private lazy val orgName =
    InfluxConfig.config.orgName.value

  def apply(timefluxClient: TimefluxClient[IO], scrapeLog: ScrapeLog[IO]): IO[InfluxWriter] =
    for
      _     <- logger.info("Initializing Influx writer...")
      _     <- logger.debug(s"Influx writer configuration: ${InfluxConfig.config}")
      orgId <- initOrg(timefluxClient).map(_.value)
      _     <- logger.debug(s"Ensuring bucket '${InfluxConfig.config.projectBucket}' exists...")
      _     <- timefluxClient.createBucketIfMissing(InfluxConfig.config.projectBucket, orgId)
      result = new InfluxWriter(timefluxClient, scrapeLog, orgId)
      _     <- logger.info(s"Initialized Influx writer on bucket ${InfluxConfig.config.projectBucket}")
    yield result

  private def initOrg(timefluxClient: TimefluxClient[IO]): IO[OrgId] =
    logger.debug(s"Ensuring organization '$orgName' exists...") >>
    timefluxClient
      .createOrgIfMissing(orgName)
      .flatMap {
        case Some(org) =>
          logger.info(s"Created organization '$orgName' with ID ${org.id}") >>
          IO.pure(OrgId(org.id))
        case None =>
          timefluxClient.resolveOrgId(InfluxConfig.config.orgName)
      }

}
