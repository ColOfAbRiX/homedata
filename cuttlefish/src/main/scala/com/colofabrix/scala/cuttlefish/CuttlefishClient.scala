package com.colofabrix.scala.cuttlefish

import cats.effect.Async
import cats.effect.std.AtomicCell
import cats.implicits.given
import com.colofabrix.scala.cuttlefish.api.*
import com.colofabrix.scala.cuttlefish.CuttlefishClient.*
import fs2.io.net.Network
import java.time.OffsetDateTime
import org.http4s.*
import org.http4s.circe.CirceEntityDecoder.*
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.client.middleware.Logger
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.Method.*
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

  implicit private val logger: SelfAwareStructuredLogger[F] =
    Slf4jLogger.getLogger[F]

  /**
   * Logs into the Octopus service
   */
  def login(apiKey: String): F[Unit] =
    logger.debug("Login") >>
    setApiKey(apiKey)

  /**
   * Logs out the Tado service
   */
  def logout(): F[Unit] =
    logger.debug("Logout") >>
    clearApiKey() >>
    clearAuthenticatedClient()

  def meterConsumption(request: MeterConsumptionRequest): F[MeterConsumptionResponse] =
    logger.debug(s"Called meterConsumption() with $request") >>
    withAuthClient().flatMap: client =>
      val url =
        (config.apiBase / s"${request.product.value}-meter-points" / request.meterPointNumber / "meters" / request.serial / "consumption")
          .withOptionQueryParam("period_from", request.from)
          .withOptionQueryParam("period_to", request.to)
          .withOptionQueryParam("page_size", request.pageSize)
          .withOptionQueryParam("order_by", request.orderBy)

      client
        .expectOr[MeterConsumptionResponse](GET(url))(handleClientExpectError)
        .flatTap: result =>
          logger.trace(s"Response for meterConsumption(): $result")

  private def withAuthClient[A](): F[Client[F]] =
    logger.trace("Getting Authenticated Client") >>
    ???

  // private def buildHttpClient(): Client[F] =
  //   Logger.colored[F](logBody = true, logHeaders = true):
  //     CuttlefishAuthenticatedClient[F](config.account):
  //       httpClient

  //  Error handlers  //

  private def handleClientExpectError(response: Response[F]): F[Throwable] =
    response
      .as[String]
      .map { body =>
        CuttlefishRequestError("Octopus Request Error", body)
      }

  //  State management  //

  // private def getApiKey(): F[Option[String]] =
  //   atomicState.get.map:
  //     _.apiKey

  private def setApiKey(apiKey: String): F[Unit] =
    atomicState.update:
      _.copy(apiKey = Some(apiKey))

  private def clearApiKey(): F[Unit] =
    atomicState.update:
      _.copy(apiKey = None)

  private def clearAuthenticatedClient(): F[Unit] =
    atomicState.update:
      _.copy(authenticatedClient = None)

  //  Givens  //

  private given QueryParamEncoder[OffsetDateTime] with
    def encode(value: OffsetDateTime): QueryParameterValue =
      QueryParameterValue(value.toString)

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
      .withTimeout(30.seconds)
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
      loggedHttpClient = Logger.colored[F](logBody = true, logHeaders = true)(httpClient)
      client           = new CuttlefishClient[F](loggedHttpClient, config, initialState)
    yield client
