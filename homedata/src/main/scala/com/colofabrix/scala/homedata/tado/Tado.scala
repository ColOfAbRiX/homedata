package com.colofabrix.scala.homedata.tado

import cats.implicits.given
import cats.effect.IO
import fs2.Chunk
import java.time.*
import org.json4s.*
import org.json4s.native.JsonMethods.*
import com.colofabrix.scala.tado4s.Tado4sClient
import com.colofabrix.scala.tado4s.api.HomeZonesResponse

class Tado private (tadoClient: Tado4sClient[IO], state: Tado.TadoState):

  def pullDate(date: LocalDate): IO[Chunk[TadoReading]] =
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

object Tado:

  private case class TadoState(
    homeId: Int,
    rooms: Map[Int, String],
  )

  def apply(): IO[Tado] =
    for
      tadoClient <- Tado4sClient.clientF[IO]()
      _          <- tadoClient.login(TadoConfig.config.username, TadoConfig.config.password)
      account    <- tadoClient.getAccountInfo()
      homeId      = account.homes.head.id
      zones      <- tadoClient.getHomeZones(homeId)
    yield
      val rooms = buildRooms(zones)
      val state = TadoState(homeId, rooms)
      new Tado(tadoClient, state)

  private def buildRooms(zones: Vector[HomeZonesResponse]): Map[Int, String] =
    zones
      .filter(_.`type` =!= "HOT_WATER")
      .map(zone => (zone.id, zone.name))
      .toMap

  def getNextDate(toDate: LocalDate, currentDate: LocalDate): Option[LocalDate] =
    if (currentDate isBefore toDate) then
      Some(currentDate.plusDays(1))
    else
      None
