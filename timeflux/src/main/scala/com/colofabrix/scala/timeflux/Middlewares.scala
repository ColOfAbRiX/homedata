package com.colofabrix.scala.timeflux

import cats.effect.kernel.Async
import cats.effect.MonadCancelThrow
import cats.implicits.given
import com.colofabrix.scala.timeflux.model.*
import org.http4s.*
import org.http4s.client.Client
import org.http4s.client.middleware.Logger
import org.typelevel.log4cats.SelfAwareLogger

/**
 * Http4s client that performs Timeflux authentication
 */
object TimefluxAuthenticatedClient:

  def apply[F[_]: MonadCancelThrow](httpClient: Client[F], token: AuthToken, orgId: OrgId): Client[F] =
    Client[F] { request =>
      val authorization = Headers("Authorization" -> s"Token ${token.value}")
      val authHeaders   = request.headers.put(authorization)
      val authUri       = request.uri.withQueryParam("orgID", orgId.value)
      val authRequest   = request.withHeaders(authHeaders).withUri(authUri)
      httpClient.run(authRequest)
    }

/**
 * Http4s client that performs conditional logging
 */
object TimefluxLoggedClient:

  def apply[F[_]: Async](httpClient: Client[F], logger: SelfAwareLogger[F]): F[Client[F]] =
    (logger.isTraceEnabled, logger.isDebugEnabled).mapN { (isTrace, isDebug) =>
      if isTrace then
        Logger.colored[F](logBody = true, logHeaders = true)(httpClient)
      else if isDebug then
        Logger.colored[F](logBody = false, logHeaders = false)(httpClient)
      else
        httpClient
    }
