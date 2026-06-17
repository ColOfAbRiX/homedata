package com.colofabrix.scala.homedata.octopus

import cats.effect.{ IO, Resource }
import cats.implicits.given
import com.colofabrix.scala.cuttlefish.{ CuttlefishClient, CuttlefishDSL, CuttlefishStreamDSL }
import com.colofabrix.scala.cuttlefish.models.*
import com.colofabrix.scala.homedata.octopus.OctopusConfig.config.*
import com.colofabrix.scala.homedata.scrape.{ ScrapeLog, ScrapeService }
import com.colofabrix.scala.homedata.utils.pipes.*
import com.colofabrix.scala.homedata.utils.fs2logging.*
import com.colofabrix.scala.timeflux.measures.*
import java.time.*
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

final class OctopusPuller private (octopusClient: CuttlefishClient[IO], scrapeLog: ScrapeLog[IO])
  extends CuttlefishDSL, CuttlefishStreamDSL {

  private enum OctopusProduct(val value: String) {
    case Electricity extends OctopusProduct("electricity")
    case Gas         extends OctopusProduct("gas")
  }

  implicit private val logger: Logger[IO] =
    Slf4jLogger.getLogger[IO]

  def pullGasReadings(from: OffsetDateTime, to: OffsetDateTime): fs2.Stream[IO, Measure] =
    pullWithFilter(from, to, OctopusProduct.Gas)
      .map {
        case MeterConsumption(consumption, intervalStart, _) =>
          OctopusReading.GasReading(intervalStart, consumption.value)
      }
      .through {
        TimefluxSerializable.toApiMeasureStream
      }
      .onFinalize {
        logger.info("Completed pull of Octopus Gas")
      }

  def pullElectricityReadings(from: OffsetDateTime, to: OffsetDateTime): fs2.Stream[IO, Measure] =
    pullWithFilter(from, to, OctopusProduct.Electricity)
      .map {
        case MeterConsumption(consumption, intervalStart, _) =>
          OctopusReading.ElectricityReading(intervalStart, consumption.value)
      }
      .through {
        TimefluxSerializable.toApiMeasureStream
      }
      .onFinalize {
        logger.info("Completed pull of Octopus Electricity")
      }

  private def pullWithFilter(
    from: OffsetDateTime,
    to: OffsetDateTime,
    product: OctopusProduct,
  ): fs2.Stream[IO, MeterConsumption] =
    fs2.Stream.info(s"Pulling Octopus ${product.value} data from=$from to=$to") ++
    generatePullGaps(product, from, to)
      .flatMap { (gapFrom, gapTo) =>
        fs2.Stream.debug(s"Pulling Octopus ${product.value} data from=$gapFrom to=$gapTo") ++
        octopusProductRequest(product, gapFrom, gapTo)
          .through {
            throttle(OctopusConfig.config.requestsPerSec)
          }
          .evalTap { result =>
            if result.intervalStart.isBefore(OffsetDateTime.now()) then
              scrapeLog.logEntry(ScrapeService.Octopus, result.intervalStart, product.value)
            else
              IO.unit
          }
      }

  private def octopusProductRequest(
    product: OctopusProduct,
    from: OffsetDateTime,
    to: OffsetDateTime,
  ): fs2.Stream[IO, MeterConsumption] =
    product match {
      case OctopusProduct.Gas =>
        octopusClient.streamGasMeterConsumption(
          GasMeterConsumptionRequest(
            mprn = OctopusConfig.config.gasMprn,
            serial = OctopusConfig.config.gasSerial,
            fromDate = Some(from),
            toDate = Some(to),
            pageSize = Some(OctopusConfig.config.pageSize),
            groupBy = None,
            orderBy = None,
            pageNumber = None,
          ),
        )
      case OctopusProduct.Electricity =>
        octopusClient.streamElectricityMeterConsumption(
          ElectricityMeterConsumptionRequest(
            mpan = OctopusConfig.config.electricityMpan,
            serial = OctopusConfig.config.electricitySerial,
            fromDate = Some(from),
            toDate = Some(to),
            pageSize = Some(OctopusConfig.config.pageSize),
            groupBy = None,
            orderBy = None,
            pageNumber = None,
          ),
        )
    }

  private def generatePullGaps(
    product: OctopusProduct,
    from: OffsetDateTime,
    to: OffsetDateTime,
  ): fs2.Stream[IO, (OffsetDateTime, OffsetDateTime)] =
    fs2.Stream.emits {
      val missingSlots =
        Iterator
          .iterate(from)(_.plusMinutes(30))
          .takeWhile(_.isBefore(to))
          .toList
          .filterNot { slot =>
            scrapeLog.contains(ScrapeService.Octopus, slot, product.value)
          }

      groupContiguousSlots(missingSlots)
    }

  private def groupContiguousSlots(slots: List[OffsetDateTime]): List[(OffsetDateTime, OffsetDateTime)] =
    slots match
      case Nil =>
        Nil
      case head :: tail =>
        tail
          .foldLeft(List((head, head))) {
            case (acc, slot) =>
              val (currentFrom, currentTo) = acc.head
              if slot == currentTo.plusMinutes(30) then
                (currentFrom, slot) :: acc.tail
              else
                (slot, slot) :: acc
          }
          .map { (gapFrom, gapTo) =>
            (gapFrom, gapTo.plusMinutes(30))
          }
          .reverse

}

object OctopusPuller {

  implicit private val logger: Logger[IO] =
    Slf4jLogger.getLogger[IO]

  def apply(cuttlefishClient: CuttlefishClient[IO], scrapeLog: ScrapeLog[IO]): IO[OctopusPuller] =
    for
      _     <- logger.info(s"Initializing Octopus puller...")
      _     <- logger.debug(s"Octopus configuration: ${OctopusConfig.config}")
      _     <- cuttlefishClient.setApiKey(OctopusConfig.config.apiKey)
      result = new OctopusPuller(cuttlefishClient, scrapeLog)
      _     <- logger.info(s"Initialized Octopus puller")
    yield result

}
