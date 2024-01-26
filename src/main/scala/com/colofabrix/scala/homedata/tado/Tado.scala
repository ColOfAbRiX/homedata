package com.colofabrix.scala.homedata.tado

import cats.implicits.given
import cats.effect.IO
import fs2.Chunk
import java.time.*
import org.json4s.*
import org.json4s.native.JsonMethods.*
import com.colofabrix.scala.tado4s.Tado4sClient

class Tado private (tadoClient: Tado4sClient[IO], state: Tado.TadoState):

  println(s"IGNORE: $tadoClient")

  def pullDate(date: LocalDate): IO[Chunk[TadoReading]] =
    state
      .roomsIds
      .flatTraverse(pullRoom(date, _))
      .map { readings =>
        Chunk.from(readings)
      }

  def sampleTadoReading(date: LocalDate, room: Int) =
    TadoReading(
      time = date.atStartOfDay(ZoneId.systemDefault()).toInstant(),
      room = s"ROOM: $room",
      atHome = true,
      windowOpen = false,
      temperature = 12.34,
      humidity = 34.21,
      outsideTemperature = 14.23,
      outsideSun = 23.14,
      setTemperature = 20,
      heatingModulation = 0.9,
    )

  private def pullRoom(date: LocalDate, roomId: Int): IO[Vector[TadoReading]] =
    println(s"IGNORE: $roomId")
    IO(Vector(sampleTadoReading(date, roomId)))
    // tadoClient
    //   .getZoneDayReport(state.homeId, roomId, date)
    //   .flatMap { report =>
    //     println(report.hoursInDay)
    //     IO(Vector(sampleTadoReading(date)))
    //   }

object Tado:

  private case class TadoState(
    homeId: Int,
    roomsIds: Vector[Int],
  )

  def apply(): IO[Tado] =
    for
      tadoClient <- Tado4sClient.clientF[IO]()
      _          <- tadoClient.login(TadoConfig.config.username, TadoConfig.config.password)
      account    <- tadoClient.getAccountInfo()
      homeId      = account.homes.head.id
      rooms      <- tadoClient.getHomeZones(homeId)
      state       = TadoState(homeId, rooms.map(_.id))
    yield new Tado(tadoClient, state)

  def getNextDate(toDate: LocalDate, currentDate: LocalDate): Option[LocalDate] =
    if (currentDate isBefore toDate) || (currentDate isEqual toDate) then
      Some(currentDate.plusDays(1))
    else
      None
