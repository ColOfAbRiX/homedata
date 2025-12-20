package com.colofabrix.scala.http4s.middleware.betterlogger

import cats.effect.*
import org.http4s.*
import org.http4s.client.*

object Http4sBeytterLogger:

  def apply[F[_]: Async](redactHeaders: Boolean = true)(client: Client[F]): Client[F] =
    ResponseLogger[F](redactHeaders) {
      RequestLogger[F](redactHeaders) {
        client
      }
    }
