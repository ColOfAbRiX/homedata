package com.colofabrix.scala.timeflux

import cats.effect.Async
import cats.implicits.toFunctorOps
import com.colofabrix.scala.timeflux.config.*
import fs2.io.net.Network
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.Uri
import cats.effect.kernel.Resource

object Timeflux:

  def clientF[F[_]: Async: Network](serverUrl: Uri, orgId: OrganizationId, token: AuthToken): F[TimefluxClient[F]] =
    EmberClientBuilder
      .default[F]
      .build
      .allocated
      .map {
        case (httpClient, _) => TimefluxClient(httpClient, serverUrl, orgId, token)
      }

  def client[F[_]: Async: Network](serverUrl: Uri, orgId: OrganizationId, token: AuthToken): Resource[F, TimefluxClient[F]] =
    EmberClientBuilder
      .default[F]
      .build
      .map { httpClient =>
        TimefluxClient(httpClient, serverUrl, orgId, token)
      }
