package com.colofabrix.scala.tado4s.security

import cats.effect.MonadCancelThrow
import org.http4s.*
import org.http4s.client.Client
import org.http4s.headers.Authorization

/**
 * Http4s client that performs Tado authentication
 */
object BearerTokenAuthClient {

  def apply[F[_]: MonadCancelThrow](bearerToken: String)(httpClient: Client[F]): Client[F] =
    Client[F] { request =>
      val authorization = Authorization(Credentials.Token(AuthScheme.Bearer, bearerToken))
      val authHeaders   = request.headers.put(authorization)
      val authRequest   = request.withHeaders(authHeaders)
      httpClient.run(authRequest)
    }

}
