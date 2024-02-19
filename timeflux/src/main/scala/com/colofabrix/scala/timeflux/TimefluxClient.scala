package com.colofabrix.scala.timeflux

import cats.effect.Async
import cats.implicits.given
import com.colofabrix.scala.restbee.*
import com.colofabrix.scala.restbee.response.*
import com.colofabrix.scala.timeflux.circe.CirceJsonCodecs.given
import com.colofabrix.scala.timeflux.config.*
import com.colofabrix.scala.timeflux.measurements.*
import com.colofabrix.scala.timeflux.model.*
import fs2.{ text, Stream }
import org.http4s.*
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.Method.*
import org.http4s.Uri.Path

/**
 * The client to interact with InfluxDB
 */
class TimefluxClient[F[_]: Async](httpClient: Client[F], config: TimefluxClientConfig)
  extends Http4sClientDsl[F]:

  private lazy val internalHttpClient: Client[F] =
    applyAuth(config.token, httpClient)

  private lazy val apiHttpClient: BeeClient[F] =
    BeeClient(internalHttpClient)

  private val bucketUri: Uri =
    (config.serverUrl / "api" / "v2" / "buckets")

  private val writeUri: Uri =
    (config.serverUrl / "api" / "v2" / "write")

  /**
   * Sets configuration parameters
   */
  def withConfig(f: TimefluxClientConfig => TimefluxClientConfig): TimefluxClient[F] =
    new TimefluxClient(httpClient, f(config))

  /**
   * Writes a stream of InfluxSerializable values in a bucket
   */
  def write[A: InfluxSerializable](bucket: String, values: Stream[F, A]): F[Unit] =
    writeStream(
      bucket = bucket,
      values = values.through(InfluxSerializable[A].toStreamMeasurements),
    )

  /**
   * Writes a stream of Measurements in a bucket
   */
  def write(bucket: String, values: Stream[F, Measurement]): F[Unit] =
    writeStream(
      bucket = bucket,
      values = values,
    )

  /**
   * Writes a list of Measurements in a bucket
   */
  def write(bucket: String, values: Measurement*): F[Unit] =
    writeStream(
      bucket = bucket,
      values = Stream(values: _*),
    )

  /**
   * List one or all the buckets
   */
  def listBuckets(name: Option[String]): F[ListBucketsResponse] =
    val request = ListBucketRequest(name)
    val url     = bucketUri.withQueryParams(request.toQueryParams)
    apiHttpClient.get[ListBucketsResponse](url)

  /**
   * Creates a bucket
   */
  def createBucket(name: String): F[CreateBucketResponse] =
    val request =
      CreateBucketRequest(
        name = name,
        orgID = config.organizationId.value,
        description = None,
        retentionRules = List.empty,
      )

    apiHttpClient.post[CreateBucketRequest, CreateBucketResponse](bucketUri, request)

  /**
   * Checks if a bucket exists and, if it doesn't, it creates it
   */
  def createBucketIfMissing(bucket: String): F[Unit] =
    for
      found <- listBuckets(Some(bucket))
      result <- if found.buckets.isEmpty then createBucket(bucket).void
                else Async[F].pure(())
    yield result

  //  Support  //

  private def writeStream(bucket: String, values: Stream[F, Measurement]): F[Unit] =
    val headers =
      Headers(
        "Content-Type" -> "text/plain; charset=utf-8",
        "Accept"       -> "application/json",
      )

    val queryRequest =
      WriteRequest(
        bucket = bucket,
        orgID = config.organizationId.value,
        precision = "ms",
      )

    val requestUri = writeUri.withQueryParams(queryRequest.toQueryParams)

    val body =
      values
        .map(_.toLineProtocol)
        .map { x =>
          println(x)
          x
        }
        .through(text.utf8.encode)

    val request = Request[F](method = POST, uri = requestUri, body = body, headers = headers)

    internalHttpClient
      .run(request)
      .use { response =>
        if response.status.isSuccess then
          Async[F].unit
        else
          response
            .decodeError[Unit, ErrorResponse]
            .compile
            .lastOrError
      }

  private def applyAuth(token: AuthToken, httpClient: Client[F]): Client[F] =
    Client { request =>
      val authorization = Headers("Authorization" -> s"Token ${token.value}")
      val authHeaders   = request.headers.put(authorization)
      val authRequest   = request.withHeaders(authHeaders)
      httpClient.run(authRequest)
    }

object TimefluxClient:

  def apply[F[_]: Async](
    http: Client[F],
    serverUrl: Uri,
    orgId: OrganizationId,
    token: AuthToken,
  ): TimefluxClient[F] =
    val config =
      TimefluxClientConfig(
        serverUrl = serverUrl.withPath(Path.empty),
        token = token,
        organizationId = orgId,
      )

    new TimefluxClient[F](http, config)
