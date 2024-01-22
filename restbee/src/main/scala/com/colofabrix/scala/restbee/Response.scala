package com.colofabrix.scala.restbee

import com.colofabrix.scala.restbee.errors.ResponseError

type ApiResponse[A] = Either[ResponseError, A]
