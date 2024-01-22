package com.colofabrix.scala.restbee

import org.http4s.Status

final class ApiError(val status: Status, val body: String) extends Throwable(body)
