package com.colofabrix.scala.cuttlefish.logger

import cats.effect.*
import org.http4s.*
import org.http4s.client.*

object Logger:

  def apply[F[_]: Async](redactHeaders: Boolean = true)(client: Client[F]): Client[F] =
    ResponseLogger[F](redactHeaders) {
      RequestLogger[F](redactHeaders) {
        client
      }
    }
