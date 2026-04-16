package com.colofabrix.scala.homedata.influx

import cats.effect.IO
import com.colofabrix.scala.homedata.utils.pipes.*
import com.colofabrix.scala.timeflux.*
import com.colofabrix.scala.timeflux.measures.Measure
import com.colofabrix.scala.timeflux.model.OrgId
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

final class InfluxWriter(timefluxClient: TimefluxClient[IO], orgId: String) extends TimefluxDSL {

  implicit private val logger: Logger[IO] =
    Slf4jLogger.getLogger[IO]

  def write(values: fs2.Stream[IO, Measure]*): IO[Unit] =
    for
      _         <- logger.info("Wiring readings into database...")
      allStreams = values.foldLeft(fs2.Stream.empty[IO])(_ merge _)
      _         <- logger.info("Starting collection of data")
      result    <- write(allStreams)
    yield result

  def write(values: fs2.Stream[IO, Measure]): IO[Unit] =
    val loggedValues =
      values.every(InfluxConfig.config.batchWrites) { _ =>
        logger.info(s"Submitted ${InfluxConfig.config.batchWrites} measures to InfluxDB")
      }

    timefluxClient.writeMeasures(
      InfluxConfig.config.projectBucket,
      orgId,
      loggedValues,
      batchWrites = Some(InfluxConfig.config.batchWrites),
    )

}

object InfluxWriter extends TimefluxDSL {

  implicit private val logger: Logger[IO] =
    Slf4jLogger.getLogger[IO]

  private lazy val orgName =
    InfluxConfig.config.orgName.value

  def apply(): IO[InfluxWriter] =
    for
      _              <- logger.info("Initializing Influx writer...")
      _              <- logger.debug(s"Influx configuration: ${InfluxConfig.config}")
      timefluxClient <- TimefluxClient[IO](InfluxConfig.clientConfig)
      orgId          <- initOrg(timefluxClient).map(_.value)
      _              <- logger.debug(s"Ensuring bucket '${InfluxConfig.config.projectBucket}' exists...")
      _              <- timefluxClient.createBucketIfMissing(InfluxConfig.config.projectBucket, orgId)
      result          = new InfluxWriter(timefluxClient, orgId)
      _              <- logger.info(s"Initialized Influx writer on bucket ${InfluxConfig.config.projectBucket}")
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
