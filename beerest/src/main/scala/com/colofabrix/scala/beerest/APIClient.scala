package com.colofabrix.scala.beerest

import cats.effect.Async
import fs2.{ text, Stream }
import io.circe.*
import io.circe.fs2.*
import io.circe.syntax.*
import org.http4s.*
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.headers.*
import org.http4s.headers.Accept
import org.http4s.Method.*

private[colofabrix] class APIClient[F[_]: Async](httpClient: Client[F]) extends Http4sClientDsl[F]:

  def postRequest[BI: Encoder, QI: GetEncoder, O: Decoder](uri: Uri, params: QI, body: BI): F[O] =
    val jsonBody      = body.asJson.noSpacesSortKeys
    val mapParams     = GetEncoder[QI].toQueryParams(params)
    val uriWithParams = uri.withQueryParams(mapParams)
    postRequest(uriWithParams, jsonBody)

  def postRequest[BI: Encoder, O: Decoder](uri: Uri, body: BI): F[O] =
    val jsonBody = body.asJson.noSpacesSortKeys
    postRequest(uri, jsonBody)

  def postRequest[O: Decoder](uri: Uri, body: String): F[O] =
    val headers =
      Headers(
        `Content-Type`(MediaType.application.json),
        Accept(MediaType.application.json),
      )

    httpClient
      .stream(POST(body, uri, headers))
      .flatMap { response =>
        if (response.status.isSuccess) then
          decodeJsonResponse(response)
        else
          handleError(response)
      }
      .compile
      .lastOrError

  def getRequest[QI: GetEncoder, O: Decoder](uri: Uri, params: QI): F[O] =
    val mapParams     = GetEncoder[QI].toQueryParams(params)
    val uriWithParams = uri.withQueryParams(mapParams)
    getRequest(uriWithParams)

  def getRequest[O: Decoder](uri: Uri): F[O] =
    val headers =
      Headers(
        `Content-Type`(MediaType.application.json),
        Accept(MediaType.application.json),
      )

    httpClient
      .stream(GET(uri, headers))
      .flatMap { response =>
        if (response.status.isSuccess) then
          decodeJsonResponse(response)
        else
          handleError(response)
      }
      .compile
      .lastOrError

  private def decodeJsonResponse[A: Decoder](response: Response[F]): Stream[F, A] =
    response
      .body
      .through(byteStreamParser)
      .through(decoder[F, A])

  private def handleError[A](response: Response[F]): Stream[F, A] =
    response
      .body
      .through(text.utf8.decode)
      .flatMap { error =>
        Stream.raiseError(ApiError(response.status, error))
      }
