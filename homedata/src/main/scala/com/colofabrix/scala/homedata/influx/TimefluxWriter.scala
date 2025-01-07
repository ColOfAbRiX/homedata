package com.colofabrix.scala.homedata.influx

import cats.effect.IO
import com.colofabrix.scala.timeflux.*
import com.colofabrix.scala.timeflux.measures.Measure
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
// import fs2.concurrent.Channel

class TimefluxWriter(timefluxClient: TimefluxClient[IO]) extends TimefluxDSL:

  def write(values: fs2.Stream[IO, Measure]*): IO[Unit] =
    timefluxClient.writeMeasures(
      InfluxDbConfig.config.projectBucket,
      values.head,
      batchWrites = Some(InfluxDbConfig.config.batchWrites),
    )
    // val singleStream = values.foldLeft(fs2.Stream.empty[IO])(_ merge _)
    // timefluxClient.writeMeasures(
    //   InfluxDbConfig.config.projectBucket,
    //   singleStream,
    //   batchWrites = Some(InfluxDbConfig.config.batchWrites),
    // )

  // def write(values: fs2.Stream[IO, Measure]*): IO[Unit] =
  //   for
  //     channel <- Channel.unbounded[IO, Measure]
  //     readers  = values.map(_.evalMap(channel.send))
  //     writer   = fs2.Stream.eval(write(channel.stream))
  //     result  <- fs2.Stream.emits(readers :+ writer).parJoinUnbounded.compile.drain
  //   yield result

  // def write(values: fs2.Stream[IO, Measure]): IO[Unit] =
  //   timefluxClient.writeMeasures(
  //     InfluxDbConfig.config.projectBucket,
  //     values,
  //     batchWrites = Some(InfluxDbConfig.config.batchWrites),
  //   )

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
