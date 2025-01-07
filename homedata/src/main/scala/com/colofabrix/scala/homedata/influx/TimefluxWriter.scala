package com.colofabrix.scala.homedata.influx

import cats.effect.IO
import com.colofabrix.scala.timeflux.*
import com.colofabrix.scala.timeflux.measures.Measure
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

class TimefluxWriter(timefluxClient: TimefluxClient[IO]) extends TimefluxDSL:

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
    timefluxClient.writeMeasures(
      InfluxDbConfig.config.projectBucket,
      values,
      batchWrites = Some(InfluxDbConfig.config.batchWrites),
    )

  // import cats.effect.kernel.Sync
  // def every[F[_]: Sync, A](n: Int)(f: A => F[Unit]): fs2.Pipe[F, A, A] =
  //   _.zipWithIndex
  //     .evalTap: (a, i) =>
  //       if i % n == 0 then f(a) else Sync[F].unit
  //     .map((a, i) => a)

object TimefluxWriter extends TimefluxDSL:

  implicit private val logger: Logger[IO] =
    Slf4jLogger.getLogger[IO]

  def apply(): IO[TimefluxWriter] =
    for
      _              <- logger.info("Initalizing Timeflux writer...")
      timefluxClient <- TimefluxClient[IO](InfluxDbConfig.clientConfig)
      _              <- timefluxClient.createBucketIfMissing(InfluxDbConfig.config.projectBucket)
      result          = new TimefluxWriter(timefluxClient)
      _              <- logger.info(s"Initalized Timeflux writer on bucket ${InfluxDbConfig.config.projectBucket}")
    yield result
