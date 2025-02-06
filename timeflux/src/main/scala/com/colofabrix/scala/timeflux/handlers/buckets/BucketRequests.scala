package com.colofabrix.scala.timeflux.handlers.buckets

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

final private[timeflux] class BucketRequestsHandler[F[_]: Async](httpClient: Client[F], baseApiUrl: Uri)
  extends Http4sClientDsl[F]:

  implicit private val logger: SelfAwareStructuredLogger[F] =
    Slf4jLogger.getLogger[F]

  def listBuckets(request: ListBucketRequest): F[ListBucketsResponse] =
    for
      _         <- logger.debug(s"Called listBuckets() with $request")
      requestUri = (baseApiUrl / "buckets").withQueryParams(request.toQueryParams)
      result    <- httpClient.expectOr[ListBucketsResponse](GET(requestUri))(handleClientExpectError)
      _         <- logger.trace(s"Response for listBuckets(): $result")
    yield result

  def createBucket(request: CreateBucketRequest): F[CreateBucketResponse] =
    for
      _         <- logger.debug(s"Called createBucket() with $request")
      requestUri = (baseApiUrl / "buckets")
      result    <- httpClient.expectOr[CreateBucketResponse](POST(request, requestUri))(handleClientExpectError)
      _         <- logger.trace(s"Response for createBucket(): $result")
    yield result

  def deleteBucket(request: DeleteBucketRequest): F[Unit] =
    for
      _         <- logger.debug(s"Called deleteBucket() with $request")
      requestUri = baseApiUrl / "buckets" / request.bucketID
      result    <- httpClient.run(DELETE(requestUri)).use(handleClientRunError).as(())
      _         <- logger.trace(s"Response for deleteBucket(): $result")
    yield result

  def createBucketIfMissing(request: CreateBucketRequest): F[Option[CreateBucketResponse]] =
    logger.debug(s"Called createBucketIfMissing() with $request") >>
    listBuckets(ListBucketRequest(Some(request.name), request.orgID))
      .attempt
      .flatMap {
        case Left(TimefluxRequestError(_, msg, _, _)) if msg.contains(s"bucket \"${request.name}\" not found") =>
          createBucket(request).map(Some(_))
        case Left(error) =>
          error.raiseError
        case Right(ListBucketsResponse(Nil, _)) =>
          createBucket(request).map(Some(_))
        case Right(_) =>
          None.pure[F]
      }
      .flatTap { result =>
        logger.trace(s"Response for listBuckets(): $result")
      }
