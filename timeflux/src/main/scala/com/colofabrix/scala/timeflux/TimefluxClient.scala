package com.colofabrix.scala.timeflux

import cats.effect.Async
import cats.effect.std.AtomicCell
import cats.implicits.given
import com.colofabrix.scala.timeflux.api.*
import com.colofabrix.scala.timeflux.model.*
import com.colofabrix.scala.timeflux.config.*
import com.colofabrix.scala.timeflux.measures.*
import com.colofabrix.scala.timeflux.TimefluxClient.*
import fs2.io.net.Network
import io.odin.*
import io.odin.formatter.Formatter
import org.http4s.*
import org.http4s.circe.CirceEntityDecoder.*
import org.http4s.circe.CirceEntityEncoder.*
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.Method.*

/**
 * InfluxDB Client for Scala
 */
final class TimefluxClient[F[_]: Async] private (
  httpClient: Client[F],
  config: TimefluxConfig,
  atomicState: AtomicCell[F, TimefluxClientState[F]],
) extends Http4sClientDsl[F]:

  private val logger: Logger[F] = consoleLogger(formatter = Formatter.colorful)

  /**
   * Logs into the Timeflux service
   */
  def login(orgId: OrgId, token: AuthToken): F[Unit] =
    setCredentials(orgId, token)

  /**
   * Logs out the Tado service
   */
  def logout(): F[Unit] =
    clearCredentials() >>
    clearAuthenticatedClient()

  /**
   * List one or all the buckets
   */
  def listBuckets(request: ListBucketRequest): F[ListBucketsResponse] =
    getApiUrl().flatMap: apiUrl =>
      useAuthClient: client =>
        val requestUri = apiUrl.withQueryParams(request.toQueryParams)
        client.expect[ListBucketsResponse](GET(requestUri))

  /**
   * Creates a bucket
   */
  def createBucket(request: CreateBucketRequest): F[CreateBucketResponse] =
    getApiUrl().flatMap: apiUrl =>
      useAuthClient: client =>
        client.expect[CreateBucketResponse](POST(request, apiUrl))

  /**
   * Checks if a bucket exists and, if it doesn't, it creates it
   */
  def createBucketIfMissing(request: CreateBucketRequest): F[Option[CreateBucketResponse]] =
    for
      found <- listBuckets(ListBucketRequest(name = Some(request.name)))
      result <- if found.buckets.isEmpty then createBucket(request).map(Some(_))
                else Async[F].pure(None)
    yield result

  /**
   * Writes a stream of TimefluxSerializable values in a bucket
   */
  def write[A: TimefluxSerializable](request: WriteRequest, values: fs2.Stream[F, A]): F[Unit] =
    writeStream(request, values.through(_.map(TimefluxSerializable[A].toMeasure)))

  //  Internal operations  //

  private def writeStream(request: WriteRequest, values: fs2.Stream[F, Measure]): F[Unit] =
    getApiUrl().flatMap: apiUrl =>
      useAuthClient: client =>
        val headers =
          Headers(
            "Content-Type" -> "text/plain; charset=utf-8",
            "Accept"       -> "application/json",
          )

        val requestUri = apiUrl.withQueryParams(request.toQueryParams)

        val body =
          values
            .map(_.toLineProtocol.value)
            .through(fs2.text.utf8.encode)

        val postRequest = Request[F](method = POST, uri = requestUri, body = body, headers = headers)

        client.expect(postRequest)

  private def useAuthClient[A](f: Client[F] => F[A]): F[A] =
    def retrieve(): F[Client[F]] =
      getAuthenticatedClient().flatMap {
        case None =>
          getCredentials().flatMap {
            case None =>
              Async[F].raiseError(TimefluxException("No Influx credentials set.", None))
            case Some(creds) =>
              val client = buildAuthenticatedClient(creds)
              setAuthenticatedClient(client) >> retrieve()
          }
        case Some(client) =>
          Async[F].pure(client)
      }

    retrieve().flatMap(f)

  private def buildAuthenticatedClient(creds: TimefluxCredentials): Client[F] =
    Client { request =>
      val authorization = Headers("Authorization" -> s"Token ${creds.token.value}")
      val authHeaders   = request.headers.put(authorization)
      val authUri       = request.uri.withQueryParam("orgID", creds.orgId.value)
      val authRequest   = request.withHeaders(authHeaders).withUri(authUri)
      httpClient.run(authRequest)
    }

  //  State management  //

  private def getCredentials(): F[Option[TimefluxCredentials]] =
    atomicState.get.map:
      _.credentials

  private def setCredentials(orgId: OrgId, token: AuthToken): F[Unit] =
    atomicState.update:
      _.copy(credentials = Some(TimefluxCredentials(token, orgId)))

  private def clearCredentials(): F[Unit] =
    atomicState.update:
      _.copy(credentials = None)

  private def getAuthenticatedClient(): F[Option[Client[F]]] =
    atomicState.get.map:
      _.useAuthClient

  private def setAuthenticatedClient(client: Client[F]): F[Unit] =
    atomicState.update:
      _.copy(useAuthClient = Some(client))

  private def clearAuthenticatedClient(): F[Unit] =
    atomicState.update:
      _.copy(useAuthClient = None)

  private def getApiUrl(): F[Uri] =
    atomicState.get.flatMap:
      _.serverUrl match {
        case None =>
          Async[F].raiseError(TimefluxException("No InfluxDB URL set.", None))
        case Some(serverUrl) =>
          Async[F].pure(serverUrl.addPath(config.apiBase))
      }

/**
 * InfluxDB Client for Scala
 */
object TimefluxClient:

  final private case class TimefluxClientState[F[_]](
    credentials: Option[TimefluxCredentials] = None,
    useAuthClient: Option[Client[F]] = None,
    serverUrl: Option[Uri] = None,
  )

  final private case class TimefluxCredentials(
    token: AuthToken,
    orgId: OrgId,
  )

  /**
   * Creates a new instance of TimefluxClient client using the given client
   */
  def apply[F[_]: Async](
    clientConfig: TimefluxClientConfig,
    httpClient: Client[F],
  ): F[TimefluxClient[F]] =
    val initialState =
      TimefluxClientState[F](
        serverUrl = Some(clientConfig.serverUrl),
        credentials = Some(TimefluxCredentials(clientConfig.authToken, clientConfig.orgId)),
      )

    for
      initialState <- AtomicCell[F].of(initialState)
      client        = new TimefluxClient[F](httpClient, TimefluxConfig.config, initialState)
    yield client

  /**
   * Creates a new instance of TimefluxClient client using http4s Ember Client
   */
  def apply[F[_]: Async: Network](clientConfig: TimefluxClientConfig): F[TimefluxClient[F]] =
    EmberClientBuilder
      .default[F]
      .build
      .allocated
      .flatMap {
        case (httpClient, _) => TimefluxClient(clientConfig, httpClient)
      }
