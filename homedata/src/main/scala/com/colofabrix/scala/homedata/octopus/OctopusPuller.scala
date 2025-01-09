package com.colofabrix.scala.homedata.octopus

import cats.effect.IO
import com.colofabrix.scala.cuttlefish.api.*
import com.colofabrix.scala.cuttlefish.CuttlefishClient
import com.colofabrix.scala.cuttlefish.CuttlefishDSL
import java.time.*
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import com.colofabrix.scala.cuttlefish.model.Throttle

class OctopusPuller private (octopusClient: CuttlefishClient[IO]) extends CuttlefishDSL:

  def pullGasReadings(from: OffsetDateTime, to: OffsetDateTime): fs2.Stream[IO, OctopusReading] =
    octopusClient
      .meterConsumption(
        product = OctopusProduct.Gas,
        meterPointNumber = OctopusConfig.config.gasMprn,
        serial = OctopusConfig.config.gasSerial,
        from = Some(from),
        to = Some(to),
        throttle = Some(Throttle(OctopusConfig.config.requestsPerSec)),
      )
      .map { consumption =>
        OctopusReading.GasReading(consumption.interval_start, consumption.consumption)
      }

  def pullElectricityReadings(from: OffsetDateTime, to: OffsetDateTime): fs2.Stream[IO, OctopusReading] =
    octopusClient
      .meterConsumption(
        product = OctopusProduct.Electricity,
        meterPointNumber = OctopusConfig.config.electricityMpan,
        serial = OctopusConfig.config.electricitySerial,
        from = Some(from),
        to = Some(to),
        throttle = Some(Throttle(OctopusConfig.config.requestsPerSec)),
      )
      .map { consumption =>
        OctopusReading.ElectricityReading(consumption.interval_start, consumption.consumption)
      }

object OctopusPuller:

  implicit private val logger: Logger[IO] =
    Slf4jLogger.getLogger[IO]

  def apply(): IO[OctopusPuller] =
    for
      _             <- logger.info(s"Initializing Octopus puller...")
      _             <- logger.debug(s"Octopus configuration: ${OctopusConfig.config}")
      octopusClient <- CuttlefishClient[IO]()
      _             <- octopusClient.login(OctopusConfig.config.apiKey)
      result         = new OctopusPuller(octopusClient)
      _             <- logger.info(s"Initialized Octopus puller")
    yield result
