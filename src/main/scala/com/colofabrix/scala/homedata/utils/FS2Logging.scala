package com.colofabrix.scala.homedata.utils

import org.typelevel.log4cats.Logger
import cats.effect.kernel.Sync

object fs2logging {

  extension [F[_]](self: fs2.Stream.type)(using logger: Logger[F]) {

    def trace(message: => String): fs2.Stream[F, Nothing] =
      fs2.Stream.exec(logger.trace(message))

    def debug(message: => String): fs2.Stream[F, Nothing] =
      fs2.Stream.exec(logger.debug(message))

    def info(message: => String): fs2.Stream[F, Nothing] =
      fs2.Stream.exec(logger.info(message))

    def warn(message: => String): fs2.Stream[F, Nothing] =
      fs2.Stream.exec(logger.warn(message))

    def error(message: => String): fs2.Stream[F, Nothing] =
      fs2.Stream.exec(logger.error(message))

  }

  extension [F[_], A](self: fs2.Stream[F, A])(using logger: Logger[F]) {

    def trace(message: A => String): fs2.Stream[F, A] =
      self.evalTap[F, Unit](x => logger.trace(message(x)))

    def debug(message: A => String): fs2.Stream[F, A] =
      self.evalTap[F, Unit](x => logger.debug(message(x)))

    def info(message: A => String): fs2.Stream[F, A] =
      self.evalTap[F, Unit](x => logger.info(message(x)))

    def warn(message: A => String): fs2.Stream[F, A] =
      self.evalTap[F, Unit](x => logger.warn(message(x)))

    def error(message: A => String): fs2.Stream[F, A] =
      self.evalTap[F, Unit](x => logger.error(message(x)))

  }

}
