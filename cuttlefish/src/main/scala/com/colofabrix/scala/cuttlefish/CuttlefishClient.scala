package com.colofabrix.scala.cuttlefish

import cats.effect.{ Async, Temporal }
import cats.effect.std.AtomicCell
import cats.implicits.given
import com.colofabrix.scala.cuttlefish.api.*
import com.colofabrix.scala.cuttlefish.CuttlefishClient.*
import com.colofabrix.scala.cuttlefish.FS2Logging.*
import com.colofabrix.scala.cuttlefish.model.*
import dev.kovstas.fs2throttler.Throttler
import fs2.io.net.Network
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import org.http4s.*
import org.http4s.circe.CirceEntityDecoder.*
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.client.middleware.*
import org.http4s.client.middleware.Logger
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.Method.*
import org.http4s.Uri.Path.SegmentEncoder
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import scala.concurrent.duration.*

/**
 * Octopus Client for Scala
 *
 * Reference: https://www.guylipman.com/octopus/api_guide.html
 */
final class CuttlefishClient[F[_]: Async] private (
  httpClient: Client[F],
  config: CuttlefishConfig,
  atomicState: AtomicCell[F, CuttlefishClientState[F]],
) extends Http4sClientDsl[F]:

  private type StreamF[+A] =
    fs2.Stream[F, A]

  implicit private val logger: SelfAwareStructuredLogger[F] =
    Slf4jLogger.getLogger[F]

  /**
   * Logs into the Octopus service
   */
  def login(apiKey: String): F[Unit] =
    logger.debug("Login") >>
    setApiKey(apiKey)

  /**
   * Logs out the Octopus service
   */
  def logout(): F[Unit] =
    logger.debug("Logout") >>
    clearApiKey() >>
    clearAuthenticatedClient()

  def pagedMeterConsumption(request: MeterConsumptionRequest): F[MeterConsumptionResponse] =
    for
      _      <- logger.debug(s"Called pagedMeterConsumption() with $request")
      result <- internalMeterConsumption(request)
      _      <- logger.trace(s"Response for pagedMeterConsumption(): $result")
    yield result

  def meterConsumption(request: MeterConsumptionRequest, throttle: Option[Throttle]): StreamF[ConsumptionResults] =
    val pageZero = request.copy(page = Some(1))
    fs2.Stream.debug(s"Called meterConsumption() with $request") >>
    fs2.Stream
      .unfoldLoopEval(pageZero) {
        pageLoopMeterConsumption(_).map((results, nextPage) => (fs2.Stream.emits(results), nextPage))
      }
      .through(fs2Throttle(throttle))
      .flatten

  private def pageLoopMeterConsumption(pageRequest: MeterConsumptionRequest) =
    logger.debug(s"Requesting ${pageRequest.product.value} meter page ${pageRequest.page.getOrElse(0)}") >>
    logger.trace(s"Requesting meter page $pageRequest") >>
    internalMeterConsumption(pageRequest).map {
      case MeterConsumptionResponse(_, Some(_), _, results) =>
        val nextPage        = pageRequest.page.map(_ + 1)
        val nextPageRequest = Option(pageRequest.copy(page = nextPage))
        (results, nextPageRequest)
      case MeterConsumptionResponse(_, None, _, results) =>
        (results, None)
    }

  private def internalMeterConsumption(request: MeterConsumptionRequest): F[MeterConsumptionResponse] =
    withAuthClient().flatMap: client =>
      val url =
        (config.apiBase / s"${request.product.value}-meter-points" / request.meterPointNumber / "meters" / request.serial / "consumption" / "")
          .withOptionQueryParam("order_by", request.orderBy)
          .withOptionQueryParam("page_size", request.pageSize)
          .withOptionQueryParam("page", request.page)
          .withOptionQueryParam("period_from", request.from)
          .withOptionQueryParam("period_to", request.to)

      client.expectOr[MeterConsumptionResponse](GET(url))(handleClientExpectError)

  //  Http Client Management  //

  private def withAuthClient[A](): F[Client[F]] =
    logger.trace("Getting Authenticated Client") >>
    atomicallyModifyAuthenticatedClient:
      case Some(client) =>
        logger.debug(s"Returning Cuttlefish authenticated client") >>
        client.pure[F]
      case None =>
        for
          _      <- logger.debug(s"Creating Cuttlefish authenticated client")
          apiKey <- getApiKey()
          client  = buildHttpClient(apiKey)
        yield client

  private def buildHttpClient(apiKey: String): Client[F] =
    val retryPolicy =
      RetryPolicy[F](
        backoff = RetryPolicy.exponentialBackoff(config.maxRetryTime, config.maxRetries),
      )

    Retry(retryPolicy):
      CuttlefishAuthenticatedClient[F](apiKey):
        httpClient

  //  Internal Methods  //

  private def fs2Throttle[F[_]: Temporal, A](throttle: Option[Throttle]): fs2.Pipe[F, A, A] =
    throttle match {
      case None =>
        identity
      case Some(requestsPerSecond) =>
        val elements = Math.max(requestsPerSecond.value, 1.0).toLong
        val duration = Math.max(1.0 / requestsPerSecond.value, 1.0).toInt
        Throttler.throttle[F, A](elements, duration.second, Throttler.Shaping)
    }

  //  Error handlers  //

  private def handleClientExpectError(response: Response[F]): F[Throwable] =
    response
      .as[CuttlefishRequestError]
      .map { error =>
        CuttlefishError("Octopus Request Error", Some(error))
      }

  //  State management  //

  private def getApiKey(): F[String] =
    atomicState.get.flatMap: state =>
      state.apiKey match
        case Some(apiKey) => apiKey.pure[F]
        case None         => CuttlefishError("No Octopus apiKey provided").raiseError

  private def setApiKey(apiKey: String): F[Unit] =
    atomicState.update:
      _.copy(apiKey = Some(apiKey))

  private def clearApiKey(): F[Unit] =
    atomicState.update:
      _.copy(apiKey = None)

  private def clearAuthenticatedClient(): F[Unit] =
    atomicState.update:
      _.copy(authenticatedClient = None)

  private def atomicallyModifyAuthenticatedClient(f: Option[Client[F]] => F[Client[F]]): F[Client[F]] =
    atomicState.evalModify: state =>
      f(state.authenticatedClient).map: newAuthenticatedClient =>
        (state.copy(authenticatedClient = Some(newAuthenticatedClient)), newAuthenticatedClient)

  //  Givens  //

  private given QueryParamEncoder[OffsetDateTime] =
    QueryParamEncoder[String].contramap(_.truncatedTo(ChronoUnit.SECONDS).toString)

  private given SegmentEncoder[MeterPointNumber] =
    SegmentEncoder[String].contramap(_.value)

  private given SegmentEncoder[SerialNumber] =
    SegmentEncoder[String].contramap(_.value)

/**
 * Cuttlefish Client for Scala
 */
object CuttlefishClient:

  final private case class CuttlefishClientState[F[_]](
    apiKey: Option[String],
    authenticatedClient: Option[Client[F]] = None,
  )

  /** Creates a new instance of Cuttlefish client using http4s Ember Client */
  def apply[F[_]: Async: Network](maybeConfig: Option[CuttlefishConfig] = None): F[CuttlefishClient[F]] =
    EmberClientBuilder
      .default[F]
      .withTimeout(maybeConfig.map(_.httpTimeout).getOrElse(30.seconds))
      .build
      .allocated
      .flatMap {
        case (httpClient, _) =>
          val config = maybeConfig.getOrElse(CuttlefishConfig.config)
          CuttlefishClient(config, httpClient)
      }

  private def apply[F[_]: Async](config: CuttlefishConfig, httpClient: Client[F]): F[CuttlefishClient[F]] =
    for
      initialState    <- AtomicCell[F].of(CuttlefishClientState[F](None))
      loggedHttpClient = Logger.colored[F](logBody = true, logHeaders = true, redactHeadersWhen = _ => false)(httpClient)
      client           = new CuttlefishClient[F](loggedHttpClient, config, initialState)
    yield client
