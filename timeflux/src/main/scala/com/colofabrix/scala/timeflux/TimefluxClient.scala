package com.colofabrix.scala.timeflux

import cats.effect.Async
import cats.effect.std.AtomicCell
import cats.implicits.given
import com.colofabrix.scala.timeflux.api.*
import com.colofabrix.scala.timeflux.config.*
import com.colofabrix.scala.timeflux.measures.*
import com.colofabrix.scala.timeflux.model.*
import com.colofabrix.scala.timeflux.TimefluxClient.*
import fs2.io.net.Network
import io.odin.*
import io.odin.formatter.Formatter
import org.http4s.*
import org.http4s.circe.CirceEntityDecoder.*
import org.http4s.circe.CirceEntityEncoder.*
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.client.middleware.Logger
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.Method.*
import scala.concurrent.duration.*

/**
 * InfluxDB Client for Scala
 */
final class TimefluxClient[F[_]: Async] private (
  httpClient: Client[F],
  config: TimefluxConfig,
  atomicState: AtomicCell[F, TimefluxClientState[F]],
) extends Http4sClientDsl[F]:

  private type StreamF[+A] = fs2.Stream[F, A]

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
    for
      baseApiUrl <- getApiUrl()
      client     <- getLoggedClient()
      requestUri  = (baseApiUrl / "buckets").withQueryParams(request.toQueryParams)
      result     <- client.expectOr[ListBucketsResponse](GET(requestUri))(handleClientExpectError)
    yield result

  /**
   * Creates a bucket
   */
  def createBucket(request: CreateBucketRequest): F[CreateBucketResponse] =
    for
      baseApiUrl <- getApiUrl()
      client     <- getLoggedClient()
      reqWithId  <- request.withOrgId(_.orgID, x => request.copy(orgID = x))
      requestUri  = baseApiUrl / "buckets"
      result     <- client.expectOr[CreateBucketResponse](POST(reqWithId, requestUri))(handleClientExpectError)
    yield result

  /**
   * Deletes a bucket
   */
  def deleteBucket(request: DeleteBucketRequest): F[Unit] =
    for
      baseApiUrl <- getApiUrl()
      client     <- getLoggedClient()
      requestUri  = baseApiUrl / "buckets" / request.bucketID
      result     <- client.run(DELETE(requestUri)).use(handleClientRunError)
    yield result

  /**
   * Checks if a bucket exists and, if it doesn't, it creates it
   */
  def createBucketIfMissing(request: CreateBucketRequest): F[Option[CreateBucketResponse]] =
    listBuckets(ListBucketRequest(name = Some(request.name)))
      .attempt
      .flatMap {
        case Left(TimefluxRequestError(_, msg, _, _)) if msg.contains(s"bucket \"${request.name}\" not found") =>
          createBucket(request).map(Some(_))
        case Left(error) =>
          Async[F].raiseError(error)
        case Right(ListBucketsResponse(Nil, _)) =>
          createBucket(request).map(Some(_))
        case Right(_) =>
          Async[F].pure(None)
      }

  /**
   * Writes a stream of TimefluxSerializable values in a bucket
   */
  def writeData[A: TimefluxSerializable](request: WriteRequest, values: StreamF[A]): F[Unit] =
    writeStream(request, values.through(_.map(TimefluxSerializable[A].toMeasure)))

  /**
   * Writes a stream of TimefluxSerializable values in a bucket
   */
  def writeMeasures(request: WriteRequest, values: StreamF[Measure]): F[Unit] =
    writeStream(request, values)

  //  Internal operations  //

  private def writeStream(request: WriteRequest, values: StreamF[Measure]): F[Unit] =
    for
      baseApiUrl     <- getApiUrl()
      client         <- getLoggedClient()
      requestWithOrg <- request.withOrgId(_.orgID, x => request.copy(orgID = x))
      result         <- sendWriteRequest(baseApiUrl, client, requestWithOrg, values)
    yield result

  private def sendWriteRequest(url: Uri, client: Client[F], request: WriteRequest, values: StreamF[Measure]): F[Unit] =
    val headers =
      Headers(
        "Content-Type" -> "text/plain; charset=utf-8",
        "Accept"       -> "application/json",
      )

    val requestUri = (url / "write").withQueryParams(request.toQueryParams)

    val body =
      values
        .map(_.toLineProtocol.value.trim + "\n")
        .evalTap(line => logger.trace(line))
        .through(fs2.text.utf8.encode)

    val postRequest =
      Request[F](
        method = POST,
        uri = requestUri,
        body = body,
        headers = headers,
      )

    client
      .run(postRequest)
      .use(handleClientRunError)

  private def getLoggedClient[A](): F[Client[F]] =
    getAuthenticatedClient().flatMap {
      case None =>
        getCredentials().flatMap {
          case None =>
            Async[F].raiseError(TimefluxException("No Influx credentials set.", None))
          case Some(creds) =>
            val client = buildHttpClient(creds)
            setAuthenticatedClient(client) >> getLoggedClient()
        }
      case Some(client) =>
        Async[F].pure(client)
    }

  extension [A](self: A)
    private def withOrgId[B](get: A => Option[String], set: Option[String] => A): F[A] =
      get(self) match {
        case Some(_) =>
          Async[F].pure(self)
        case None =>
          getCredentials().flatMap {
            case Some(TimefluxCredentials(_, orgId)) =>
              Async[F].pure(set(Some(orgId.value)))
            case None =>
              Async[F].raiseError(TimefluxException("Required OrgID parameter is not set.", None))
          }
      }

  private def buildHttpClient(creds: TimefluxCredentials): Client[F] =
    Logger(logBody = true, logHeaders = true) {
      Client { request =>
        val authorization = Headers("Authorization" -> s"Token ${creds.token.value}")
        val authHeaders   = request.headers.put(authorization)
        val authUri       = request.uri.withQueryParam("orgID", creds.orgId.value)
        val authRequest   = request.withHeaders(authHeaders).withUri(authUri)
        httpClient.run(authRequest)
      }
    }

  //  Error handlers  //

  private def handleClientRunError(response: Response[F]): F[Unit] =
    if (response.status.isSuccess)
      logger.trace(s"Successfull request with response ${response.as[String]}") >>
      Async[F].unit
    else
      logger.error(s"Error request with response ${response.as[String]}") >>
      response
        .as[TimefluxRequestError]
        .flatMap(Async[F].raiseError)

  private def handleClientExpectError(response: Response[F]): F[Throwable] =
    response
      .as[TimefluxRequestError]
      .map(_.asInstanceOf[Throwable])

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
    AtomicCell[F]
      .of {
        TimefluxClientState[F](
          serverUrl = Some(clientConfig.serverUrl),
          credentials = Some(TimefluxCredentials(clientConfig.authToken, clientConfig.orgId)),
        )
      }
      .map { initialAtomicState =>
        new TimefluxClient[F](httpClient, TimefluxConfig.config, initialAtomicState)
      }

  /**
   * Creates a new instance of TimefluxClient client using http4s Ember Client
   */
  def apply[F[_]: Async: Network](clientConfig: TimefluxClientConfig): F[TimefluxClient[F]] =
    EmberClientBuilder
      .default[F]
      .withTimeout(30.seconds)
      .build
      .allocated
      .flatMap {
        case (httpClient, _) => TimefluxClient(clientConfig, httpClient)
      }
