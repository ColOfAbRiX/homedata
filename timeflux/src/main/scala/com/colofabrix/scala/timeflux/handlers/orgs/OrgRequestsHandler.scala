package com.colofabrix.scala.timeflux.handlers.orgs

import cats.effect.Async
import cats.implicits.given
import com.colofabrix.scala.timeflux.api.*
import com.colofabrix.scala.timeflux.handlers.ErrorHandling.*
import org.http4s.*
import org.http4s.circe.CirceEntityDecoder.*
import org.http4s.circe.CirceEntityEncoder.*
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.Method.*
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

final private[timeflux] class OrgRequestsHandler[F[_]: Async](httpClient: Client[F], baseApiUrl: Uri)
  extends Http4sClientDsl[F] {

  implicit private val logger: SelfAwareStructuredLogger[F] =
    Slf4jLogger.getLogger[F]

  def listOrgs(request: ListOrgsRequest): F[ListOrgsResponse] =
    for
      _         <- logger.debug(s"Called listOrgs() with $request")
      requestUri = (baseApiUrl / "orgs").withQueryParams(request.toQueryParams)
      result    <- httpClient.expectOr[ListOrgsResponse](GET(requestUri))(handleClientExpectError)
      _         <- logger.trace(s"Response for listOrgs(): $result")
    yield result

  def createOrg(request: CreateOrgRequest): F[CreateOrgResponse] =
    for
      _         <- logger.debug(s"Called createOrg() with $request")
      requestUri = baseApiUrl / "orgs"
      result    <- httpClient.expectOr[CreateOrgResponse](POST(request, requestUri))(handleClientExpectError)
      _         <- logger.trace(s"Response for createOrg(): $result")
    yield result

  def createOrgIfMissing(request: CreateOrgRequest): F[Option[CreateOrgResponse]] =
    logger.debug(s"Called createOrgIfMissing() with $request") >>
    listOrgs(ListOrgsRequest(Some(request.name), None))
      .attempt
      .flatMap {
        case Left(TimefluxRequestError(_, msg, _, _)) if msg.contains(s"organization name \"${request.name}\" not found") =>
          createOrg(request).map(Some(_))
        case Left(error) =>
          error.raiseError
        case Right(ListOrgsResponse(Nil, _)) =>
          createOrg(request).map(Some(_))
        case Right(_) =>
          None.pure[F]
      }
      .flatTap { result =>
        logger.trace(s"Response for createOrgIfMissing(): $result")
      }

}
