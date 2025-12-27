package com.colofabrix.scala.homedata.octopus

import cats.effect.{ IO, Resource }
import cats.implicits.given
import com.colofabrix.scala.cuttlefish.api.*
import com.colofabrix.scala.cuttlefish.CuttlefishClient
import com.colofabrix.scala.cuttlefish.CuttlefishDSL
import com.colofabrix.scala.cuttlefish.model.{ MeterPointNumber, SerialNumber, Throttle }
import com.colofabrix.scala.homedata.scrape.{ ScrapeLog, ScrapeService }
import com.colofabrix.scala.homedata.utils.pipes.*
import java.time.*
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

class OctopusPuller private (octopusClient: CuttlefishClient[IO], scrapeLog: ScrapeLog[IO]) extends CuttlefishDSL {
  import com.colofabrix.scala.homedata.octopus.OctopusConfig.config.*

  implicit private val logger: Logger[IO] =
    Slf4jLogger.getLogger[IO]

  def pullGasReadings(from: OffsetDateTime, to: OffsetDateTime): fs2.Stream[IO, OctopusReading] =
    pullWithFilter(from, to, "gas", OctopusProduct.Gas, gasMprn, gasSerial)
      .map { c =>
        OctopusReading.GasReading(c.interval_start, c.consumption)
      }

  def pullElectricityReadings(from: OffsetDateTime, to: OffsetDateTime): fs2.Stream[IO, OctopusReading] =
    pullWithFilter(from, to, "electricity", OctopusProduct.Electricity, electricityMpan, electricitySerial)
      .map { c =>
        OctopusReading.ElectricityReading(c.interval_start, c.consumption)
      }

  private def pullWithFilter(
    from: OffsetDateTime,
    to: OffsetDateTime,
    meterType: String,
    product: OctopusProduct,
    meterPointNumber: MeterPointNumber,
    serial: SerialNumber,
  ): fs2.Stream[IO, ConsumptionResults] =
    val missingSlots =
      generateTimeSlots(from, to).filterNot { slot =>
        scrapeLog.contains(ScrapeService.Octopus, slot, meterType)
      }

    if missingSlots.isEmpty then
      fs2.Stream.empty
    else
      val adjustedFrom = missingSlots.min
      val adjustedTo   = missingSlots.max.plusMinutes(30)

      val request =
        MeterConsumptionRequest(
          product = product,
          meterPointNumber = meterPointNumber,
          serial = serial,
          from = Some(adjustedFrom),
          to = Some(adjustedTo),
          pageSize = Some(OctopusConfig.config.pageSize),
          page = Some(1),
          orderBy = None,
        )

      fs2.Stream.exec(logger.info(s"Pulling Octopus $meterType data from=$adjustedFrom to=$adjustedTo")) ++
      streamReadings(request).evalTap { result =>
        if result.interval_start.isBefore(OffsetDateTime.now()) then
          scrapeLog.logEntry(ScrapeService.Octopus, result.interval_start, meterType)
        else
          IO.unit
      }

  private def generateTimeSlots(from: OffsetDateTime, to: OffsetDateTime): List[OffsetDateTime] =
    Iterator
      .iterate(from)(_.plusMinutes(30))
      .takeWhile(_.isBefore(to))
      .toList

  private def streamReadings(request: MeterConsumptionRequest): fs2.Stream[IO, ConsumptionResults] =
    fs2.Stream
      .unfoldLoopEval(request) { pageRequest =>
        octopusClient.meterConsumption(pageRequest)
          .map {
            case MeterConsumptionResponse(_, Some(_), _, results) =>
              val nextPage        = pageRequest.page.map(_ + 1)
              val nextPageRequest = Option(pageRequest.copy(page = nextPage))
              (fs2.Stream.emits(results), nextPageRequest)

            case MeterConsumptionResponse(_, None, _, results) =>
              (fs2.Stream.emits(results), None)
          }
      }
      .through(throttle(OctopusConfig.config.requestsPerSec))
      .flatten

}

object OctopusPuller {

  implicit private val logger: Logger[IO] =
    Slf4jLogger.getLogger[IO]

  def apply(scrapeLog: ScrapeLog[IO]): IO[OctopusPuller] =
    for
      _             <- logger.info(s"Initializing Octopus puller...")
      _             <- logger.debug(s"Octopus configuration: ${OctopusConfig.config}")
      octopusClient <- CuttlefishClient[IO]()
      _             <- octopusClient.login(OctopusConfig.config.apiKey)
      result         = new OctopusPuller(octopusClient, scrapeLog)
      _             <- logger.info(s"Initialized Octopus puller")
    yield result

}
