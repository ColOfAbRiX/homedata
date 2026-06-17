package com.colofabrix.scala.homedata.tado

import cats.effect.IO
import cats.implicits.given
import com.colofabrix.scala.homedata.models.DataEntry
import com.colofabrix.scala.homedata.scrape.{ ScrapeLog, ScrapeService }
import com.colofabrix.scala.homedata.tado.readings.*
import com.colofabrix.scala.homedata.utils.pipes.*
import com.colofabrix.scala.tado4s.api.HomeZoneResponse
import com.colofabrix.scala.tado4s.Tado4sClient
import com.colofabrix.scala.timeflux.measures.*
import java.time.*
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

class TadoPuller private (tadoClient: Tado4sClient[IO], scrapeLog: ScrapeLog[IO], state: TadoPuller.TadoState) {

  implicit private val logger: Logger[IO] =
    Slf4jLogger.getLogger[IO]

  def pullReadings(from: OffsetDateTime, to: OffsetDateTime): fs2.Stream[IO, DataEntry] =
    fs2.Stream
      .unfold(from.toLocalDate)(generateNextDate(to))
      .flatMap(collectRoomIds)
      .filter((date, roomId) => !scrapeLog.contains(ScrapeService.Tado, toTimestamp(date), roomId.toString))
      .through(throttle(TadoConfig.config.requestsPerSec))
      .map(pullRoom)
      .parJoinUnbounded
      .map { reading =>
        val measure = reading.toMeasure
        DataEntry(measure, ScrapeService.Tado, measure.time, reading.room.get)
      }
      .onFinalize(logger.info("Completed pull of Tado data"))

  private def generateNextDate(to: OffsetDateTime)(current: LocalDate): Option[(LocalDate, LocalDate)] =
    if (current.isBefore(to.toLocalDate) || current.isEqual(to.toLocalDate)) then
      Some(current, current.plusDays(1))
    else
      None

  private def collectRoomIds(date: LocalDate): fs2.Stream[IO, (LocalDate, Int)] =
    fs2.Stream.emits(state.rooms.keys.toList.map(date -> _))

  private def pullRoom(date: LocalDate, roomId: Int): fs2.Stream[IO, TadoReading] =
    fs2.Stream.evals {
      for {
        _        <- logger.info(s"Pulling Tado Room data for date=$date, roomId=$roomId")
        report   <- tadoClient.getZoneDayReport(state.homeId, roomId, date)
        readings <- ReportConverter.convert(state.rooms(roomId), report)
      } yield readings
    }

  private def toTimestamp(date: LocalDate): OffsetDateTime =
    date
      .atStartOfDay
      .atOffset(ZoneOffset.UTC)

}

object TadoPuller {

  private case class TadoState(
    homeId: Int,
    rooms: Map[Int, String],
  )

  implicit private val logger: Logger[IO] =
    Slf4jLogger.getLogger[IO]

  def apply(tadoClient: Tado4sClient[IO], scrapeLog: ScrapeLog[IO]): IO[TadoPuller] =
    for
      _           <- logger.info("Initializing Tado puller...")
      _           <- logger.debug(s"Tado configuration: ${TadoConfig.config}")
      _           <- tadoClient.authenticate(TadoConfig.config.initialRefreshToken)
      account     <- tadoClient.getAccountInfo()
      homeId       = account.homes.head.id
      zones       <- tadoClient.getHomeZones(homeId)
      _           <- logger.info(s"Initialized Tado puller: account=${account.email}, homeId=$homeId, zones=${zones.map(_.id)}")
      initialState = TadoState(homeId, buildRoomsList(zones))
      result       = new TadoPuller(tadoClient, scrapeLog, initialState)
    yield result

  private def buildRoomsList(zones: Vector[HomeZoneResponse]): Map[Int, String] =
    zones
      .filter(_.`type` =!= "HOT_WATER")
      .map(zone => (zone.id, zone.name))
      .toMap

}
