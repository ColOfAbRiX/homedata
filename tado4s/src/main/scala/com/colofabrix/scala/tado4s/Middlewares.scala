package com.colofabrix.scala.tado4s

import cats.effect.kernel.Async
import cats.effect.MonadCancelThrow
import cats.implicits.given
import org.http4s.*
import org.http4s.client.Client
import org.http4s.client.middleware.Logger
import org.http4s.headers.Authorization
import org.typelevel.log4cats.SelfAwareLogger

/**
 * Http4s client that performs Tado authentication
 */
object TadoAuthenticatedClient:

  def apply[F[_]: MonadCancelThrow](httpClient: Client[F], bearerToken: String): Client[F] =
    Client[F] { request =>
      val authorization = Authorization(Credentials.Token(AuthScheme.Bearer, bearerToken))
      val authHeaders   = request.headers.put(authorization)
      val authRequest   = request.withHeaders(authHeaders)
      httpClient.run(authRequest)
    }

/**
 * Http4s client that performs conditional logging
 */
object TadoLoggedClient:

  def apply[F[_]: Async](httpClient: Client[F], logger: SelfAwareLogger[F]): F[Client[F]] =
    (logger.isTraceEnabled, logger.isDebugEnabled).mapN { (isTrace, isDebug) =>
      if isTrace then
        Logger.colored[F](logBody = true, logHeaders = true)(httpClient)
      else if isDebug then
        Logger.colored[F](logBody = false, logHeaders = false)(httpClient)
      else
        httpClient
    }
