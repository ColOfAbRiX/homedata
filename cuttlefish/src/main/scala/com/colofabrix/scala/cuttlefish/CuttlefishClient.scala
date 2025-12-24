package com.colofabrix.scala.cuttlefish

import cats.effect.{ Async, Temporal }
import cats.effect.std.AtomicCell
import cats.implicits.given
import com.colofabrix.scala.cuttlefish.api.*
import com.colofabrix.scala.cuttlefish.CuttlefishClient.*
import com.colofabrix.scala.cuttlefish.FS2Logging.*
import com.colofabrix.scala.http4s.middleware.betterlogger.Logger
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

  //  Account Endpoint  //

  /** Retrieves account information */
  def getAccount(request: AccountRequest): F[AccountResponse] =
    logger.debug(s"Getting account ${request.accountNumber}") >>
    withAuthClient().flatMap: client =>
      val url = config.apiBase / "accounts" / request.accountNumber / ""
      client.expectOr[AccountResponse](GET(url))(handleClientExpectError)

  //  Products Endpoints (No Auth Required)  //

  /** Retrieves list of all products */
  def getProducts(request: ProductsRequest): F[ProductsResponse] =
    logger.debug("Getting products list") >>
    internalGetProducts(request)

  private def internalGetProducts(request: ProductsRequest): F[ProductsResponse] =
    val url =
      (config.apiBase / "products" / "")
        .withOptionQueryParam("brand", request.brand)
        .withOptionQueryParam("is_variable", request.isVariable)
        .withOptionQueryParam("is_business", request.isBusiness)
        .withOptionQueryParam("is_green", request.isGreen)
        .withOptionQueryParam("is_prepay", request.isPrepay)
        .withOptionQueryParam("available_at", request.availableAt)
    httpClient.expectOr[ProductsResponse](GET(url))(handleClientExpectError)

  /** Retrieves details for a specific product */
  def getProductDetails(request: ProductDetailsRequest): F[ProductDetailsResponse] =
    logger.debug(s"Getting product details for ${request.productCode}") >>
    internalGetProductDetails(request)

  private def internalGetProductDetails(request: ProductDetailsRequest): F[ProductDetailsResponse] =
    val url =
      (config.apiBase / "products" / request.productCode / "")
        .withOptionQueryParam("tariffs_active_at", request.tariffsActiveAt)
    httpClient.expectOr[ProductDetailsResponse](GET(url))(handleClientExpectError)

  //  Grid Supply Points Endpoint (No Auth Required)  //

  /** Retrieves grid supply points for a postcode */
  def getGridSupplyPoints(request: GridSupplyPointsRequest): F[GridSupplyPointsResponse] =
    logger.debug(s"Getting grid supply points for postcode ${request.postcode}") >> {
      val url = (config.apiBase / "industry" / "grid-supply-points" / "").withQueryParam("postcode", request.postcode.value)
      httpClient.expectOr[GridSupplyPointsResponse](GET(url))(handleClientExpectError)
    }

  //  Electricity Meter Point Endpoint (No Auth Required)  //

  /** Retrieves information about an electricity meter point */
  def getElectricityMeterPoint(request: ElectricityMeterPointRequest): F[ElectricityMeterPointResponse] =
    logger.debug(s"Getting electricity meter point for mpan ${request.mpan}") >> {
      val url = config.apiBase / "electricity-meter-points" / request.mpan / ""
      httpClient.expectOr[ElectricityMeterPointResponse](GET(url))(handleClientExpectError)
    }

  //  Electricity Tariff Endpoints (No Auth Required)  //

  /** Retrieves standard unit rates for an electricity tariff */
  def getElectricityStandardUnitRates(request: ElectricityUnitRatesRequest): F[UnitRatesResponse] =
    logger.debug(s"Getting electricity standard unit rates for ${request.productCode}/${request.tariffCode}") >>
    internalGetElectricityTariffRates("standard-unit-rates", request)

  /** Retrieves day unit rates for an electricity tariff (Economy 7) */
  def getElectricityDayUnitRates(request: ElectricityUnitRatesRequest): F[UnitRatesResponse] =
    logger.debug(s"Getting electricity day unit rates for ${request.productCode}/${request.tariffCode}") >>
    internalGetElectricityTariffRates("day-unit-rates", request)

  /** Retrieves night unit rates for an electricity tariff (Economy 7) */
  def getElectricityNightUnitRates(request: ElectricityUnitRatesRequest): F[UnitRatesResponse] =
    logger.debug(s"Getting electricity night unit rates for ${request.productCode}/${request.tariffCode}") >>
    internalGetElectricityTariffRates("night-unit-rates", request)

  private def internalGetElectricityTariffRates(rateType: String, request: ElectricityUnitRatesRequest): F[UnitRatesResponse] =
    val url =
      (config.apiBase / "products" / request.productCode / "electricity-tariffs" / request.tariffCode / rateType / "")
        .withOptionQueryParam("period_from", request.periodFrom)
        .withOptionQueryParam("period_to", request.periodTo)
        .withOptionQueryParam("page_size", request.pageSize)
    httpClient.expectOr[UnitRatesResponse](GET(url))(handleClientExpectError)

  /** Retrieves standing charges for an electricity tariff */
  def getElectricityStandingCharges(request: ElectricityStandingChargesRequest): F[StandingChargesResponse] =
    logger.debug(s"Getting electricity standing charges for ${request.productCode}/${request.tariffCode}") >>
    internalGetElectricityStandingCharges(request)

  private def internalGetElectricityStandingCharges(request: ElectricityStandingChargesRequest): F[StandingChargesResponse] =
    val url =
      (config.apiBase / "products" / request.productCode / "electricity-tariffs" / request.tariffCode / "standing-charges" / "")
        .withOptionQueryParam("period_from", request.periodFrom)
        .withOptionQueryParam("period_to", request.periodTo)
        .withOptionQueryParam("page_size", request.pageSize)
    httpClient.expectOr[StandingChargesResponse](GET(url))(handleClientExpectError)

  //  Gas Tariff Endpoints (No Auth Required)  //

  /** Retrieves standard unit rates for a gas tariff */
  def getGasStandardUnitRates(request: GasUnitRatesRequest): F[UnitRatesResponse] =
    logger.debug(s"Getting gas standard unit rates for ${request.productCode}/${request.tariffCode}") >>
    internalGetGasTariffRates(request)

  private def internalGetGasTariffRates(request: GasUnitRatesRequest): F[UnitRatesResponse] =
    val url =
      (config.apiBase / "products" / request.productCode / "gas-tariffs" / request.tariffCode / "standard-unit-rates" / "")
        .withOptionQueryParam("period_from", request.periodFrom)
        .withOptionQueryParam("period_to", request.periodTo)
        .withOptionQueryParam("page_size", request.pageSize)
    httpClient.expectOr[UnitRatesResponse](GET(url))(handleClientExpectError)

  /** Retrieves standing charges for a gas tariff */
  def getGasStandingCharges(request: GasStandingChargesRequest): F[StandingChargesResponse] =
    logger.debug(s"Getting gas standing charges for ${request.productCode}/${request.tariffCode}") >>
    internalGetGasStandingCharges(request)

  private def internalGetGasStandingCharges(request: GasStandingChargesRequest): F[StandingChargesResponse] =
    val url =
      (config.apiBase / "products" / request.productCode / "gas-tariffs" / request.tariffCode / "standing-charges" / "")
        .withOptionQueryParam("period_from", request.periodFrom)
        .withOptionQueryParam("period_to", request.periodTo)
        .withOptionQueryParam("page_size", request.pageSize)
    httpClient.expectOr[StandingChargesResponse](GET(url))(handleClientExpectError)

  //  Http Client Management  //

  private def withAuthClient(): F[Client[F]] =
    logger.trace("Getting Authenticated Client") >>
    atomicState.evalModify { state =>
      state.authenticatedClient match
        case Some(client) =>
          logger.debug("Returning Cuttlefish authenticated client").as((state, client))
        case None =>
          state.apiKey match
            case None =>
              CuttlefishError("No Octopus apiKey provided. Call login() first.")
                .raiseError[F, (CuttlefishClientState[F], Client[F])]
            case Some(apiKey) =>
              val newClient = buildHttpClient(apiKey)
              val newState  = state.copy(authenticatedClient = Some(newClient))
              logger.debug("Creating Cuttlefish authenticated client").as((newState, newClient))
    }

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

  private def setApiKey(apiKey: String): F[Unit] =
    atomicState.update(_.copy(apiKey = Some(apiKey), authenticatedClient = None))

  private def clearApiKey(): F[Unit] =
    atomicState.update(_.copy(apiKey = None, authenticatedClient = None))

  private def clearAuthenticatedClient(): F[Unit] =
    atomicState.update(_.copy(authenticatedClient = None))

  //  Givens  //

  private given QueryParamEncoder[OffsetDateTime] =
    QueryParamEncoder[String].contramap(_.truncatedTo(ChronoUnit.SECONDS).toString)

  private given SegmentEncoder[MeterPointNumber] =
    SegmentEncoder[String].contramap(_.value)

  private given SegmentEncoder[SerialNumber] =
    SegmentEncoder[String].contramap(_.value)

  private given SegmentEncoder[AccountNumber] =
    SegmentEncoder[String].contramap(_.value)

  private given SegmentEncoder[ProductCode] =
    SegmentEncoder[String].contramap(_.value)

  private given SegmentEncoder[TariffCode] =
    SegmentEncoder[String].contramap(_.value)

  private given SegmentEncoder[Mpan] =
    SegmentEncoder[String].contramap(_.value)

/**
 * Cuttlefish Client for Scala
 */
object CuttlefishClient:

  final private case class CuttlefishClientState[F[_]](
    apiKey: Option[String],
    authenticatedClient: Option[Client[F]] = None,
  )

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
      loggedHttpClient = Logger()(httpClient)
      client           = new CuttlefishClient[F](loggedHttpClient, config, initialState)
    yield client
