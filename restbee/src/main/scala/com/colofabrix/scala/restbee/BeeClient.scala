package com.colofabrix.scala.restbee

import cats.effect.Async
import cats.implicits.given
import com.colofabrix.scala.restbee.encoding.{JsonDecoder, JsonEncoder}
import com.colofabrix.scala.restbee.errors.ResponseError
import fs2.{ text, Stream }
import org.http4s.*
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.Method.*

final class BeeClient[F[_]: Async](httpClient: Client[F]) extends Http4sClientDsl[F]:

  def post[I: JsonEncoder, O: JsonDecoder](uri: Uri, body: I, headers: Option[Headers] = None): F[ApiResponse[O]] =
    val restHeaders =
      Headers("Content-Type" -> "application/json", "Accept" -> "application/json") ++ headers.getOrElse(Headers.empty)

    val jsonBody = JsonEncoder[I].encode(body)

    httpClient
      .stream(POST(jsonBody, uri, restHeaders))
      .flatMap { response =>
        if (response.status.isSuccess) then
          decodeJsonResponse(response)
        else
          handleError(response)
      }
      .compile
      .lastOrError

  def get[O: JsonDecoder](uri: Uri, headers: Option[Headers] = None): F[ApiResponse[O]] =
    val restHeaders =
      Headers("Content-Type" -> "application/json", "Accept" -> "application/json") ++ headers.getOrElse(Headers.empty)

    httpClient
      .stream(GET(uri, restHeaders))
      .flatMap { response =>
        if (response.status.isSuccess) then
          decodeJsonResponse(response)
        else
          handleError(response)
      }
      .compile
      .lastOrError

  private def decodeJsonResponse[O: JsonDecoder](response: Response[F]): Stream[F, ApiResponse[O]] =
    response
      .body
      .through(JsonDecoder[O].decodeByteStream)
      .map(_.asRight)

  private def handleError[O, E: JsonDecoder](response: Response[F]): Stream[F, ApiResponse[O]] =
    response
      .body
      .through(text.utf8.decode)
      .map { error =>
        ResponseError(response.status, error).asLeft
      }
