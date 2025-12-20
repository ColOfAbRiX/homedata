package com.colofabrix.scala.http4s.middleware.betterlogger

import cats.kernel.Order
import cats.syntax.contravariant.*

/**
 * Log levels for the HTTP4s logger middleware.
 * Higher levels include more detail in the output.
 */
enum LogLevel(val level: Int) extends Ordered[LogLevel]:
  case Trace extends LogLevel(5)
  case Debug extends LogLevel(4)
  case Info  extends LogLevel(3)
  case Warn  extends LogLevel(2)
  case Error extends LogLevel(1)
  case Off   extends LogLevel(0)

  def compare(that: LogLevel): Int =
    this.level - that.level

object LogLevel:
  given Order[LogLevel] =
    Order[Int].contramap[LogLevel](_.level)
