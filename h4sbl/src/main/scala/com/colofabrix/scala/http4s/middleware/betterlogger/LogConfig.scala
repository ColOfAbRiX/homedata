package com.colofabrix.scala.http4s.middleware.betterlogger

import scala.Console.*

/**
 * Configuration for the HTTP client logger
 */
final case class LogConfig(
  /** Whether to redact sensitive headers like Authorization */
  redactHeaders: Boolean = true,
  /** Color scheme for log output */
  colors: LogColors = LogColors.default,
  /** Whether to log request bodies */
  logRequestBody: Boolean = true,
  /** Whether to log response bodies */
  logResponseBody: Boolean = true,
)

object LogConfig:
  val default: LogConfig = LogConfig()

/**
 * Color scheme for log output
 */
final case class LogColors(
  httpVersion: String = WHITE,
  safeMethod: String = GREEN,
  unsafeMethod: String = YELLOW,
  uri: String = s"$MAGENTA$BOLD",
  headers: String = BLUE,
  body: String = WHITE,
  successStatus: String = GREEN,
  clientErrorStatus: String = YELLOW,
  serverErrorStatus: String = RED,
  reset: String = RESET,
)

object LogColors:
  val default: LogColors = LogColors()
  val noColors: LogColors = LogColors("", "", "", "", "", "", "", "", "", "")
