package com.colofabrix.scala.homedata.tado

import cats.effect.{ IO, Temporal }
import cats.implicits.given
import com.colofabrix.scala.homedata.tado.readings.*
import com.colofabrix.scala.tado4s.api.HomeZonesResponse
import com.colofabrix.scala.tado4s.Tado4sClient
import dev.kovstas.fs2throttler.Throttler
import java.time.*
import org.json4s.native.JsonMethods.*
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import scala.concurrent.duration.*

class TadoPuller private (tadoClient: Tado4sClient[IO], state: TadoPuller.TadoState):

  implicit private val logger: Logger[IO] =
    Slf4jLogger.getLogger[IO]

  def pullReadings(from: OffsetDateTime, to: OffsetDateTime): fs2.Stream[IO, TadoReading] =
    fs2.Stream
      .unfold(from.toLocalDate)(generateNextDate(to))
      .flatMap(collectRoomIds)
      .through(throttle)
      .map(pullRoom)
      .parJoinUnbounded

  private def throttle[F[_]: Temporal, A] =
    val elements = Math.max(TadoConfig.config.requestsPerSec, 1).toInt
    val duration = Math.max(1.0 / TadoConfig.config.requestsPerSec, 1).toInt
    Throttler.throttle[F, A](elements, duration.second, Throttler.Shaping)

  private def generateNextDate(to: OffsetDateTime)(current: LocalDate): Option[(LocalDate, LocalDate)] =
    if (current.isBefore(to.toLocalDate) || current.isEqual(to.toLocalDate)) then
      Some(current, current.plusDays(1))
    else
      None

  private def collectRoomIds(date: LocalDate): fs2.Stream[IO, (LocalDate, Int)] =
    fs2.Stream.emits {
      state
        .rooms
        .keys
        .toList
        .map(date -> _)
    }

  private def pullRoom(date: LocalDate, roomId: Int): fs2.Stream[IO, TadoReading] =
    fs2.Stream.evals {
      logger.info(s"Pulling Tado Room data for date=$date, roomId=$roomId") >>
      tadoClient
        .getZoneDayReport(state.homeId, roomId, date)
        .flatMap(ReportConverter.convert(state.rooms(roomId), _))
    }

object TadoPuller:

  private case class TadoState(
    homeId: Int,
    rooms: Map[Int, String],
  )

  implicit private val logger: Logger[IO] =
    Slf4jLogger.getLogger[IO]

  def apply(): IO[TadoPuller] =
    for
      _           <- logger.info(s"Initializing Tado puller...")
      tadoClient  <- Tado4sClient[IO](None)
      _           <- tadoClient.login(TadoConfig.config.username, TadoConfig.config.password)
      account     <- tadoClient.getAccountInfo()
      homeId       = account.homes.head.id
      zones       <- tadoClient.getHomeZones(homeId)
      _           <- logger.info(s"Initialized Tado puller: account=${account.email}, homeId=$homeId, zones=${zones.map(_.id)}")
      initialState = TadoState(homeId, buildRoomsList(zones))
      result       = new TadoPuller(tadoClient, initialState)
    yield result

  private def buildRoomsList(zones: Vector[HomeZonesResponse]): Map[Int, String] =
    zones
      .filter(_.`type` =!= "HOT_WATER")
      .map(zone => (zone.id, zone.name))
      .toMap
