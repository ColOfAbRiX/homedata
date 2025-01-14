package com.colofabrix.scala.homedata.influx

import cats.effect.IO
import com.colofabrix.scala.homedata.utils.pipes.*
import com.colofabrix.scala.timeflux.*
import com.colofabrix.scala.timeflux.measures.Measure
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

class InfluxWriter(timefluxClient: TimefluxClient[IO]) extends TimefluxDSL:

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
      loggedValues,
      batchWrites = Some(InfluxConfig.config.batchWrites),
    )

object InfluxWriter extends TimefluxDSL:

  implicit private val logger: Logger[IO] =
    Slf4jLogger.getLogger[IO]

  def apply(): IO[InfluxWriter] =
    for
      _              <- logger.info("Initializing Influx writer...")
      _              <- logger.debug(s"Influx configuration: ${InfluxConfig.config}")
      timefluxClient <- TimefluxClient[IO](InfluxConfig.clientConfig)
      _              <- timefluxClient.createBucketIfMissing(InfluxConfig.config.projectBucket)
      result          = new InfluxWriter(timefluxClient)
      _              <- logger.info(s"Initialized Influx writer on bucket ${InfluxConfig.config.projectBucket}")
    yield result
