package com.colofabrix.scala.timeflux

import cats.effect.Async
import cats.effect.std.AtomicCell
import cats.implicits.given
import com.colofabrix.scala.timeflux.api.*
import com.colofabrix.scala.timeflux.config.*
import com.colofabrix.scala.timeflux.handlers.buckets.*
import com.colofabrix.scala.timeflux.handlers.query.*
import com.colofabrix.scala.timeflux.handlers.write.*
import com.colofabrix.scala.timeflux.logger.*
import com.colofabrix.scala.timeflux.measures.*
import com.colofabrix.scala.timeflux.model.*
import com.colofabrix.scala.timeflux.TimefluxClient.*
import fs2.io.net.Network
import org.http4s.*
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.Method.*
import scala.concurrent.duration.*

/**
 * InfluxDB Client for Scala
 *
 * Reference: https://docs.influxdata.com/influxdb/v2/api/
 */
final class TimefluxClient[F[_]: Async] private (
  httpClient: Client[F],
  config: TimefluxConfig,
  atomicState: AtomicCell[F, TimefluxClientState[F]],
) extends Http4sClientDsl[F]:

  private type StreamF[+A] =
    fs2.Stream[F, A]

  private val authenticator: TimefluxAuthentication[F] =
    new TimefluxAuthentication(httpClient, config, atomicState)

  /**
   * Logs into the Timeflux service
   */
  def login(orgId: OrgId, token: AuthToken): F[Unit] =
    authenticator.login(orgId, token)

  /**
   * Logs out the Timeflux service
   */
  def logout(): F[Unit] =
    authenticator.logout()

  /**
   * List one or all the buckets
   */
  def listBuckets(request: ListBucketRequest): F[ListBucketsResponse] =
    handleRequest(request) { (client, baseApiUrl, req) =>
      BucketRequestsHandler(client, baseApiUrl).listBuckets(req)
    }

  /**
   * Creates a bucket
   */
  def createBucket(request: CreateBucketRequest): F[CreateBucketResponse] =
    handleRequest(request) { (client, baseApiUrl, req) =>
      BucketRequestsHandler(client, baseApiUrl).createBucket(req)
    }

  /**
   * Deletes a bucket
   */
  def deleteBucket(request: DeleteBucketRequest): F[Unit] =
    handleRequest(request) { (client, baseApiUrl, req) =>
      BucketRequestsHandler(client, baseApiUrl).deleteBucket(req)
    }

  /**
   * Checks if a bucket exists and, if it doesn't, it creates it
   */
  def createBucketIfMissing(request: CreateBucketRequest): F[Option[CreateBucketResponse]] =
    handleRequest(request) { (client, baseApiUrl, req) =>
      BucketRequestsHandler(client, baseApiUrl).createBucketIfMissing(req)
    }

  /**
   * Writes a stream of TimefluxSerializable values in a bucket
   */
  def writeData[A: TimefluxSerializable](request: WriteRequest, values: StreamF[A]): F[Unit] =
    handleRequest(request) { (client, baseApiUrl, req) =>
      WriteRequestHandler(client, config, baseApiUrl)
        .writeRequest(request, values.through(TimefluxSerializable.toApiMeasureStream))
    }

  /**
   * Writes a stream of TimefluxSerializable values in a bucket
   */
  def writeMeasures(request: WriteRequest, values: StreamF[Measure]): F[Unit] =
    handleRequest(request) { (client, baseApiUrl, req) =>
      WriteRequestHandler(client, config, baseApiUrl).writeRequest(request, values)
    }

  /**
   * Queries InfluxDB and returns a stream of results
   */
  def query(request: QueryRequest): F[StreamF[ResultRow]] =
    handleRequest(request) { (client, baseApiUrl, req) =>
      QueryRequestHandler(client, baseApiUrl).queryRequest(req)
    }

  //  Internal operations  //

  private def handleRequest[A, B](request: A)(f: (Client[F], Uri, A) => F[B]): F[B] =
    for
      baseApiUrl <- getApiUrl()
      client     <- authenticator.withAuthClient()
      result     <- f(client, baseApiUrl, request)
    yield result

  //  State management  //

  private def getApiUrl(): F[Uri] =
    atomicState.get.flatMap:
      _.serverUrl match {
        case None =>
          TimefluxException("No InfluxDB URL set.", None).raiseError
        case Some(serverUrl) =>
          serverUrl.addPath(config.apiBase).pure[F]
      }

/**
 * InfluxDB Client for Scala
 */
object TimefluxClient:

  final private[timeflux] case class TimefluxClientState[F[_]](
    credentials: Option[TimefluxCredentials] = None,
    authenticatedClient: Option[Client[F]] = None,
    serverUrl: Option[Uri] = None,
  )

  final private[timeflux] case class TimefluxCredentials(
    token: AuthToken,
    orgId: OrgId,
  )

  /**
   * Creates a new instance of TimefluxClient client using http4s Ember Client
   */
  def apply[F[_]: Async: Network](
    clientConfig: TimefluxClientConfig,
    maybeConfig: Option[TimefluxConfig] = None,
  ): F[TimefluxClient[F]] =
    EmberClientBuilder
      .default[F]
      .withTimeout(maybeConfig.map(_.httpTimeout).getOrElse(30.seconds))
      .build
      .allocated
      .flatMap {
        case (httpClient, _) =>
          val config = maybeConfig.getOrElse(TimefluxConfig.config)
          TimefluxClient(config, clientConfig, httpClient)
      }

  private def apply[F[_]: Async](
    config: TimefluxConfig,
    clientConfig: TimefluxClientConfig,
    httpClient: Client[F],
  ): F[TimefluxClient[F]] =
    for
      atomicState     <- AtomicCell[F].of(initialState[F](clientConfig))
      loggedHttpClient = Logger[F](redactHeaders = false)(httpClient)
      client           = new TimefluxClient[F](loggedHttpClient, config, atomicState)
    yield client

  private def initialState[F[_]](clientConfig: TimefluxClientConfig): TimefluxClientState[F] =
    TimefluxClientState[F](
      serverUrl = Some(clientConfig.serverUrl),
      credentials = Some(TimefluxCredentials(clientConfig.authToken, clientConfig.orgId)),
    )
