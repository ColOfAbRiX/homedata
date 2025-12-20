package com.colofabrix.scala.timeflux

import cats.effect.MonadCancelThrow
import com.colofabrix.scala.timeflux.model.*
import org.http4s.*
import org.http4s.client.Client

/**
 * Http4s client that performs Timeflux authentication
 */
object TimefluxAuthenticatedClient:

  def apply[F[_]: MonadCancelThrow](token: AuthToken)(httpClient: Client[F]): Client[F] =
    Client[F] { request =>
      val authorization = Headers("Authorization" -> s"Token ${token.value}")
      val authHeaders   = request.headers.put(authorization)
      val authRequest   = request.withHeaders(authHeaders)

      httpClient.run(authRequest)
    }
