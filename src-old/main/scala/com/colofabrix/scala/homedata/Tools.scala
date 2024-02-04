package com.colofabrix.scala.homedata

import cats.effect.IO
import fs2.{ Chunk, Stream }
import io.odin.*

object Tools:

  private val logger: Logger[IO] = consoleLogger()

  type PagePull[A] = Int => IO[(Chunk[A], Boolean)]

  def pullPaged[A](pull: PagePull[A]): Stream[IO, A] =
    Stream
      .unfoldLoopEval(1) { pageNumber =>
        pull(pageNumber).map {
          case (readings, true)  => (readings, Some(pageNumber + 1))
          case (readings, false) => (readings, None)
        }
      }
      .flatMap(Stream.chunk)
      .evalTap { reading =>
        logger.debug(s"Pulled reading: $reading")
      }

  type NextPull[I, O] = I => IO[Chunk[O]]

  def pullRange[I, O](start: I, getNext: I => Option[I])(pull: NextPull[I, O]): Stream[IO, O] =
    Stream
      .unfoldLoopEval(start) { current =>
        pull(current).map((_, getNext(current)))
      }
      .flatMap(Stream.chunk)
      .evalTap { reading =>
        logger.debug(s"Pulled reading: $reading")
      }
