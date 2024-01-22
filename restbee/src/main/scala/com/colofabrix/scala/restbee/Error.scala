package com.colofabrix.scala.restbee.errors

import cats.MonadThrow
import com.colofabrix.scala.restbee.*
import org.http4s.Status

final class ResponseError(val status: Status, val body: String)
  extends Throwable(s"Error ${status.code} in the REST response: $body")

def raiseErrorResponse[F[_]: MonadThrow, O](response: ApiResponse[O]): F[O] =
  response match
    case Left(error)  => MonadThrow[F].raiseError(error)
    case Right(value) => MonadThrow[F].pure(value)
