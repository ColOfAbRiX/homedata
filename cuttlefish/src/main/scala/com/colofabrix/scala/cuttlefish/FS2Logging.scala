package com.colofabrix.scala.cuttlefish

import org.typelevel.log4cats.SelfAwareStructuredLogger

private[cuttlefish] object FS2Logging:

  extension [F[_]](self: fs2.Stream.type)(using logger: SelfAwareStructuredLogger[F]) {

    def trace(message: => String): fs2.Stream[F, Unit] = fs2.Stream.eval(logger.trace(message))
    def debug(message: => String): fs2.Stream[F, Unit] = fs2.Stream.eval(logger.debug(message))
    def info(message: => String): fs2.Stream[F, Unit]  = fs2.Stream.eval(logger.info(message))
    def warn(message: => String): fs2.Stream[F, Unit]  = fs2.Stream.eval(logger.warn(message))
    def error(message: => String): fs2.Stream[F, Unit] = fs2.Stream.eval(logger.error(message))

  }
