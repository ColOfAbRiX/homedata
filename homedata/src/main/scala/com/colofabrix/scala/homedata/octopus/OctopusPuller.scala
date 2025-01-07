package com.colofabrix.scala.homedata.octopus

import cats.effect.{ IO, Temporal }
import com.colofabrix.scala.cuttlefish.api.*
import com.colofabrix.scala.cuttlefish.CuttlefishClient
import com.colofabrix.scala.cuttlefish.CuttlefishDSL
import dev.kovstas.fs2throttler.Throttler
import java.time.*
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import scala.concurrent.duration.*

class OctopusPuller private (octopusClient: CuttlefishClient[IO]) extends CuttlefishDSL:

  def pullGasReadings(from: OffsetDateTime, to: OffsetDateTime): fs2.Stream[IO, OctopusReading] =
    octopusClient
      .meterConsumption(
        OctopusProduct.Gas,
        OctopusConfig.config.gasMprn,
        OctopusConfig.config.gasSerial,
        Some(from),
        Some(to),
      )
      .through(throttle)
      .map { consumption =>
        OctopusReading.GasReading(consumption.interval_start, consumption.consumption)
      }

  def pullElectricityReadings(from: OffsetDateTime, to: OffsetDateTime): fs2.Stream[IO, OctopusReading] =
    octopusClient
      .meterConsumption(
        OctopusProduct.Electricity,
        OctopusConfig.config.electricityMpan,
        OctopusConfig.config.electricitySerial,
        Some(from),
        Some(to),
      )
      .through(throttle)
      .map { consumption =>
        OctopusReading.ElectricityReading(consumption.interval_start, consumption.consumption)
      }

  private def throttle[F[_]: Temporal, A] =
    val elements = Math.max(OctopusConfig.config.requestsPerSec, 1).toInt
    val duration = Math.max(1.0 / OctopusConfig.config.requestsPerSec, 1).toInt
    Throttler.throttle[F, A](elements, duration.second, Throttler.Shaping)

object OctopusPuller:

  implicit private val logger: Logger[IO] =
    Slf4jLogger.getLogger[IO]

  def apply(): IO[OctopusPuller] =
    for
      _             <- logger.info(s"Initializing Octopus puller...")
      octopusClient <- CuttlefishClient[IO]()
      _             <- octopusClient.login(OctopusConfig.config.apiKey)
      result         = new OctopusPuller(octopusClient)
    yield result
