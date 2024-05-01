package com.colofabrix.scala.timeflux.api

import org.http4s.Status

class TimefluxException(message: String, inner: Option[Throwable] = None) extends Throwable(message):
  inner.foreach(super.addSuppressed)
