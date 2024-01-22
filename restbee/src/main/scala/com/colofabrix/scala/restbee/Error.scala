package com.colofabrix.scala.restbee.errors

import cats.effect.Async
import cats.MonadThrow
import com.colofabrix.scala.restbee.*
import com.colofabrix.scala.restbee.encoding.JsonDecoder
import fs2.{ text, Stream }
import org.http4s.Response
import org.http4s.Status

final class ResponseError(val status: Status, val body: String)
  extends Throwable(s"Error ${status.code} in the REST response: $body")

final class ResponseTypedError[E](val status: Status, error: E)
  extends Throwable(s"Error ${status.code} in the REST response: $error")

final class CodecError(message: String, inner: Option[Throwable] = None) extends Throwable(message):
  inner.foreach(super.addSuppressed)

def handleError[F[_]: MonadThrow, O](errorResponse: Response[F]): Stream[F, O] =
  errorResponse
    .body
    .through(text.utf8.decode)
    .flatMap { error =>
      Stream.raiseError(ResponseError(errorResponse.status, error))
    }

def handleErrorTyped[F[_]: Async, O, E: JsonDecoder](errorResponse: Response[F]): Stream[F, O] =
  errorResponse
    .body
    .through(JsonDecoder[E].decodeByteStream)
    .flatMap { error =>
      Stream.raiseError(ResponseTypedError(errorResponse.status, error))
    }
