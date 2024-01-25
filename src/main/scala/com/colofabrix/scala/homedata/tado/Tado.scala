package com.colofabrix.scala.homedata.tado

import cats.implicits.given
import cats.effect.IO
import fs2.Chunk
import java.time.*
import org.json4s.*
import org.json4s.native.JsonMethods.*
import com.colofabrix.scala.tado4s.Tado4sClient

class Tado private (tadoClient: Tado4sClient[IO], state: Tado.TadoState):

  def pullDate(date: LocalDate): IO[Chunk[TadoReading]] =
    state
      .roomsIds
      .flatTraverse(pullRoom(date, _))
      .map { readings =>
        Chunk.from(readings)
      }

  private def pullRoom(date: LocalDate, roomId: Int): IO[Vector[TadoReading]] =
    tadoClient
      .getDayReport(state.homeId, roomId, date)
      .flatMap { report =>
        println(report)
        ???
      }

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
