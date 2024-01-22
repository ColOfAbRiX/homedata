package com.colofabrix.scala.restbee

import cats.effect.Async
import com.colofabrix.scala.restbee.encoding.{ JsonDecoder, JsonEncoder }
import com.colofabrix.scala.restbee.errors.*
import com.colofabrix.scala.restbee.response.*
import org.http4s.*
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.Method.*

/**
 * Sting-free REST API Client and tools
 */
final class BeeClient[F[_]: Async](httpClient: Client[F]) extends Http4sClientDsl[F]:

  /**
   * Send a POST request and decode the result into an effect, raise any error in the MonadError
   */
  def post[I: JsonEncoder, O: JsonDecoder](uri: Uri, body: I, headers: Option[Headers] = None): F[O] =
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

  /**
   * Send a POST request and decode the result into an effect, raise any error in the MonadError
   */
  def get[O: JsonDecoder](uri: Uri, headers: Option[Headers] = None): F[O] =
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
