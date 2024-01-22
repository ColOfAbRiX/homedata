package com.colofabrix.scala.timeflux

import cats.Applicative
import cats.effect.Async
import cats.implicits.given
import com.colofabrix.scala.restbee.*
import com.colofabrix.scala.restbee.encoding.CirceJsonCodecs.given
import com.colofabrix.scala.restbee.errors.*
import com.colofabrix.scala.timeflux.config.*
import com.colofabrix.scala.timeflux.MeasurementWriter.*
import com.colofabrix.scala.timeflux.model.*
import fs2.{ text, Stream }
import org.http4s.*
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.headers.*
import org.http4s.Method.*
import org.http4s.Uri.Path

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
   */
  def withConfig(f: TimefluxClientConfig => TimefluxClientConfig): TimefluxClient[F] =
    new TimefluxClient(httpClient, f(config))

  /**
   */
  def write[A: InfluxSerializable](bucket: String, values: Stream[F, A]): F[Unit] =
    writeStream(
      bucket = bucket,
      values = values.through(InfluxSerializable[A].toStreamMeasurements),
    )

  /**
   */
  def write(bucket: String, values: Stream[F, Measurement]): F[Unit] =
    writeStream(
      bucket = bucket,
      values = values,
    )

  /**
   */
  def write(bucket: String, values: Measurement*): F[Unit] =
    writeStream(
      bucket = bucket,
      values = Stream(values: _*),
    )

  /**
   */
  def listBuckets(name: Option[String]): F[ListBucketsResponse] =
    val request = ListBucketRequest(name)
    val url     = bucketUri.withQueryParams(request.toQueryParams)
    apiHttpClient
      .get[ListBucketsResponse](url)
      .flatMap(raiseErrorResponse)

  /**
   */
  def createBucket(name: String): F[CreateBucketResponse] =
    val request =
      CreateBucketRequest(
        name = name,
        orgID = config.organizationId.value,
        description = None,
        retentionRules = List.empty,
      )

    apiHttpClient
      .post[CreateBucketRequest, CreateBucketResponse](bucketUri, request)
      .flatMap(raiseErrorResponse)

  /**
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
        bucket,
        config.organizationId.value,
        "ms",
      )

    val requestUri = bucketUri.withQueryParams(queryRequest.toQueryParams)

    val body =
      values
        .map(_.toLineProtocol)
        .through(text.utf8.encode)

    val request = Request[F](method = POST, uri = requestUri, body = body, headers = headers)

    internalHttpClient
      .run(request)
      .use { response =>
        if (response.status.isSuccess)
          Async[F].unit
        else
          handleError(response)
            .as(())
            .compile
            .lastOrError
      }

  private def handleError[A](response: Response[F]): Stream[F, A] =
    import io.circe.*
    import io.circe.fs2.*
    response
      .body
      .through(byteStreamParser)
      .through(decoder[F, ErrorResponse])
      .flatMap { error =>
        Stream.raiseError(InfluxRestError(response.status, error))
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
