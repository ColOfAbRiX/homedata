package com.colofabrix.scala.beerest

import org.http4s.Status

final class ApiError(val status: Status, val body: String) extends Throwable
