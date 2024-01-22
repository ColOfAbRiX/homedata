package com.colofabrix.scala.timeflux.model

import org.http4s.Status

class InfluxException(message: String, inner: Option[Throwable]) extends Throwable(message):
  inner.foreach(super.addSuppressed)

class InfluxRestError(status: Status, error: ErrorResponse, inner: Option[Throwable])
  extends InfluxException(s"$status: $error", inner)
