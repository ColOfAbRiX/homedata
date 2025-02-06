package com.colofabrix.scala.cuttlefish.logger

import cats.syntax.all.given
import cats.*
import org.typelevel.log4cats.SelfAwareStructuredLogger
import cats.kernel.Order

enum LogLevel(val level: Int):
  case Trace extends LogLevel(5)
  case Debug extends LogLevel(4)
  case Info  extends LogLevel(3)
  case Warn  extends LogLevel(2)
  case Error extends LogLevel(1)
  case Off   extends LogLevel(0)

object LogLevel:

  given Order[LogLevel] =
    Order[Int].contramap[LogLevel](_.level)

  extension [F[_]: Monad](self: SelfAwareStructuredLogger[F]) {
    def currentLogLevel(): F[LogLevel] =
      (self.isTraceEnabled, self.isDebugEnabled, self.isInfoEnabled, self.isWarnEnabled, self.isErrorEnabled).mapN {
        case (isTraceEnabled, isDebugEnabled, isInfoEnabled, isWarnEnabled, isErrorEnabled) =>
          if (isTraceEnabled) LogLevel.Trace
          else if (isDebugEnabled) LogLevel.Debug
          else if (isInfoEnabled) LogLevel.Info
          else if (isWarnEnabled) LogLevel.Warn
          else if (isErrorEnabled) LogLevel.Error
          else LogLevel.Off
      }

    def logMax(message: => String): F[Unit] =
      currentLogLevel().flatMap {
        case Trace => self.trace(message)
        case Debug => self.debug(message)
        case Info  => self.info(message)
        case Warn  => self.warn(message)
        case Error => self.error(message)
        case Off   => Monad[F].unit
      }

  }
