package com.colofabrix.scala.homedata.octopus

import cats.effect.IO
import fs2.{ Chunk, Stream }
import io.odin.*
import java.time.OffsetDateTime

object Octopus:

  private type PagePull[A] = Int => IO[(Chunk[A], Boolean)]

  private val logger: Logger[IO] = consoleLogger()

  def pullReadings(from: OffsetDateTime): Stream[IO, OctopusReading] =
    val electricityReadings = pullPaged(OctopusElectricity.pullPage(from))
    val gasReadings         = pullPaged(OctopusGas.pullPage(from))

    (electricityReadings merge gasReadings)

  private def pullPaged[A](pull: PagePull[A]): Stream[IO, A] =
    Stream
      .unfoldLoopEval(1) { pageNumber =>
        pull(pageNumber).map {
          case (readings, true)  => (readings, Some(pageNumber + 1))
          case (readings, false) => (readings, None)
        }
      }
      .flatMap(Stream.chunk)
      .evalTap(reading => logger.debug(s"Octopus reading: $reading"))
