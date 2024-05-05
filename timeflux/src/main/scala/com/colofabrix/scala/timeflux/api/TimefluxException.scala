package com.colofabrix.scala.timeflux.api

import io.circe.*
import io.circe.derivation.*
import org.http4s.Status

class TimefluxException(message: String, inner: Option[Throwable] = None) extends Throwable(message):
  inner.foreach(super.addSuppressed)

case class TimefluxRequestError(
  code: String,
  message: String,
  err: Option[String],
  op: Option[String],
) extends TimefluxException(message, None) derives Decoder
