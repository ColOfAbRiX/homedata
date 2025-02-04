package com.colofabrix.scala.timeflux.handlers

import cats.effect.kernel.Async
import cats.implicits.given
import com.colofabrix.scala.timeflux.api.TimefluxRequestError
import org.http4s.*
import org.http4s.circe.CirceEntityDecoder.*

private[timeflux] object ErrorHandling:

  def handleClientStreamError[F[_]: Async](response: Response[F]): fs2.Stream[F, Unit] =
    fs2.Stream.eval(handleClientRunError(response))

  def handleClientRunError[F[_]: Async](response: Response[F]): F[Unit] =
    if (response.status.isSuccess)
      Async[F].unit
    else
      handleClientExpectError(response) >>= Async[F].raiseError

  def handleClientExpectError[F[_]: Async](response: Response[F]): F[Throwable] =
    response
      .as[TimefluxRequestError]
      .map(_.asInstanceOf[Throwable])
