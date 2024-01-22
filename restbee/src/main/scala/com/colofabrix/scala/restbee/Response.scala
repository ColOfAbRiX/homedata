package com.colofabrix.scala.restbee.response

import cats.effect.Async
import com.colofabrix.scala.restbee.encoding.*
import com.colofabrix.scala.restbee.errors.*
import fs2.Stream
import org.http4s.*

type ApiResponse[A] = Either[ResponseError, A]

/**
  * Decodes a JSON response into its type
  */
def decodeJsonResponse[F[_]: Async, O: JsonDecoder](response: Response[F]): Stream[F, O] =
  response
    .body
    .through(JsonDecoder[O].decodeByteStream)

extension [F[_]: Async](response: Response[F])
  def decodeJson[O: JsonDecoder]: Stream[F, O] =
    decodeJsonResponse(response)

  def decodeError[O, E: JsonDecoder]: Stream[F, O] =
    handleErrorTyped[F, O, E](response)

  def handle[O: JsonDecoder, E: JsonDecoder]: Stream[F, O] =
    if response.status.isSuccess then
      response.decodeJson[O]
    else
      response.decodeError[O, E]
