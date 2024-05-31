package com.colofabrix.scala.homedata.tado

import cats.effect.IO
import cats.implicits.given
import com.colofabrix.scala.homedata.tado.readings.*
import com.colofabrix.scala.tado4s.api.HomeZonesResponse
import com.colofabrix.scala.tado4s.Tado4sClient
import fs2.{ Chunk, Stream }
import io.odin.*
import java.time.*
import org.json4s.*
import org.json4s.native.JsonMethods.*

class TadoPuller private (tadoClient: Tado4sClient[IO], state: TadoPuller.TadoState):

  private val logger: Logger[IO] = consoleLogger()

  def pullReadings(from: OffsetDateTime, to: OffsetDateTime): Stream[IO, TadoReading] =
    Stream
      .unfoldLoopEval(from.toLocalDate) { currentDate =>
        val nextDate =  if (currentDate.isBefore(to.toLocalDate)) then Some(currentDate.plusDays(1)) else None
        pullDate(currentDate).map((_, nextDate))
      }
      .flatMap(Stream.chunk)
      .evalTap(reading => logger.trace(reading.show))

  private def pullDate(date: LocalDate): IO[Chunk[TadoReading]] =
    state
      .rooms
      .keys
      .toVector
      .flatTraverse(pullRoom(date, _))
      .map { readings =>
        Chunk.from(readings)
      }

  private def pullRoom(date: LocalDate, roomId: Int): IO[Vector[TadoReading]] =
    tadoClient
      .getZoneDayReport(state.homeId, roomId, date)
      .flatMap(ReportConverter.convert(state.rooms(roomId), _))

object TadoPuller:

  private case class TadoState(
    homeId: Int,
    rooms: Map[Int, String],
  )

  def apply(): IO[TadoPuller] =
    for
      tadoClient <- Tado4sClient[IO](None)
      _          <- tadoClient.login(TadoConfig.config.username, TadoConfig.config.password)
      account    <- tadoClient.getAccountInfo()
      homeId      = account.homes.head.id
      zones      <- tadoClient.getHomeZones(homeId)
    yield
      val rooms = buildRoomsList(zones)
      val state = TadoState(homeId, rooms)
      new TadoPuller(tadoClient, state)

  private def buildRoomsList(zones: Vector[HomeZonesResponse]): Map[Int, String] =
    zones
      .filter(_.`type` =!= "HOT_WATER")
      .map(zone => (zone.id, zone.name))
      .toMap
