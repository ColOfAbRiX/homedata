package com.colofabrix.scala.timeflux

import cats.effect.MonadCancelThrow
import com.colofabrix.scala.timeflux.model.*
import org.http4s.*
import org.http4s.client.Client

/**
 * Http4s client that performs Timeflux authentication
 */
object TimefluxAuthenticatedClient:

  def apply[F[_]: MonadCancelThrow](token: AuthToken, orgId: OrgId)(httpClient: Client[F]): Client[F] =
    Client[F] { request =>
      val requestOrgId =
        request
          .uri
          .params
          .get("orgID")
          .getOrElse(orgId.value)

      val authorization = Headers("Authorization" -> s"Token ${token.value}")
      val authHeaders   = request.headers.put(authorization)
      val authUri       = request.uri.withQueryParam("orgID", requestOrgId)
      val authRequest   = request.withHeaders(authHeaders).withUri(authUri)

      httpClient.run(authRequest)
    }
