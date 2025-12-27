package com.colofabrix.scala.cuttlefish

import com.colofabrix.scala.cuttlefish.api.*
import com.colofabrix.scala.cuttlefish.model.*
import java.time.OffsetDateTime

trait CuttlefishDSL:

  extension [F[_]](cuttlefishClient: CuttlefishClient[F])

    def meterConsumption(
      product: OctopusProduct,
      meterPointNumber: MeterPointNumber,
      serial: SerialNumber,
      from: Option[OffsetDateTime] = None,
      to: Option[OffsetDateTime] = None,
      pageSize: Option[Int] = None,
      page: Option[Int] = None,
      orderBy: Option[String] = None,
    ): F[MeterConsumptionResponse] =
      cuttlefishClient.meterConsumption(
        MeterConsumptionRequest(
          product = product,
          meterPointNumber = meterPointNumber,
          serial = serial,
          from = from,
          to = to,
          pageSize = pageSize,
          page = page,
          orderBy = orderBy,
        ),
      )

    //  Account  //

    def account(accountNumber: AccountNumber): F[AccountResponse] =
      cuttlefishClient.getAccount(AccountRequest(accountNumber = accountNumber))

    //  Products  //

    def products(
      brand: Option[String] = None,
      isVariable: Option[Boolean] = None,
      isBusiness: Option[Boolean] = None,
      isGreen: Option[Boolean] = None,
      isPrepay: Option[Boolean] = None,
      availableAt: Option[OffsetDateTime] = None,
    ): F[ProductsResponse] =
      cuttlefishClient.getProducts(
        ProductsRequest(
          brand = brand,
          isVariable = isVariable,
          isBusiness = isBusiness,
          isGreen = isGreen,
          isPrepay = isPrepay,
          availableAt = availableAt,
        ),
      )

    def productDetails(
      productCode: ProductCode,
      tariffsActiveAt: Option[OffsetDateTime] = None,
    ): F[ProductDetailsResponse] =
      cuttlefishClient.getProductDetails(
        ProductDetailsRequest(
          productCode = productCode,
          tariffsActiveAt = tariffsActiveAt,
        ),
      )

    //  Grid Supply Points  //

    def gridSupplyPoints(postcode: Postcode): F[GridSupplyPointsResponse] =
      cuttlefishClient.getGridSupplyPoints(GridSupplyPointsRequest(postcode = postcode))

    //  Electricity Meter Points  //

    def electricityMeterPoint(mpan: Mpan): F[ElectricityMeterPointResponse] =
      cuttlefishClient.getElectricityMeterPoint(ElectricityMeterPointRequest(mpan = mpan))

    //  Electricity Tariff Rates  //

    def electricityStandardUnitRates(
      productCode: ProductCode,
      tariffCode: TariffCode,
      periodFrom: Option[OffsetDateTime] = None,
      periodTo: Option[OffsetDateTime] = None,
      pageSize: Option[Int] = None,
    ): F[UnitRatesResponse] =
      cuttlefishClient.getElectricityStandardUnitRates(
        ElectricityUnitRatesRequest(
          productCode = productCode,
          tariffCode = tariffCode,
          periodFrom = periodFrom,
          periodTo = periodTo,
          pageSize = pageSize,
        ),
      )

    def electricityDayUnitRates(
      productCode: ProductCode,
      tariffCode: TariffCode,
      periodFrom: Option[OffsetDateTime] = None,
      periodTo: Option[OffsetDateTime] = None,
      pageSize: Option[Int] = None,
    ): F[UnitRatesResponse] =
      cuttlefishClient.getElectricityDayUnitRates(
        ElectricityUnitRatesRequest(
          productCode = productCode,
          tariffCode = tariffCode,
          periodFrom = periodFrom,
          periodTo = periodTo,
          pageSize = pageSize,
        ),
      )

    def electricityNightUnitRates(
      productCode: ProductCode,
      tariffCode: TariffCode,
      periodFrom: Option[OffsetDateTime] = None,
      periodTo: Option[OffsetDateTime] = None,
      pageSize: Option[Int] = None,
    ): F[UnitRatesResponse] =
      cuttlefishClient.getElectricityNightUnitRates(
        ElectricityUnitRatesRequest(
          productCode = productCode,
          tariffCode = tariffCode,
          periodFrom = periodFrom,
          periodTo = periodTo,
          pageSize = pageSize,
        ),
      )

    def electricityStandingCharges(
      productCode: ProductCode,
      tariffCode: TariffCode,
      periodFrom: Option[OffsetDateTime] = None,
      periodTo: Option[OffsetDateTime] = None,
      pageSize: Option[Int] = None,
    ): F[StandingChargesResponse] =
      cuttlefishClient.getElectricityStandingCharges(
        ElectricityStandingChargesRequest(
          productCode = productCode,
          tariffCode = tariffCode,
          periodFrom = periodFrom,
          periodTo = periodTo,
          pageSize = pageSize,
        ),
      )

    //  Gas Tariff Rates  //

    def gasStandardUnitRates(
      productCode: ProductCode,
      tariffCode: TariffCode,
      periodFrom: Option[OffsetDateTime] = None,
      periodTo: Option[OffsetDateTime] = None,
      pageSize: Option[Int] = None,
    ): F[UnitRatesResponse] =
      cuttlefishClient.getGasStandardUnitRates(
        GasUnitRatesRequest(
          productCode = productCode,
          tariffCode = tariffCode,
          periodFrom = periodFrom,
          periodTo = periodTo,
          pageSize = pageSize,
        ),
      )

    def gasStandingCharges(
      productCode: ProductCode,
      tariffCode: TariffCode,
      periodFrom: Option[OffsetDateTime] = None,
      periodTo: Option[OffsetDateTime] = None,
      pageSize: Option[Int] = None,
    ): F[StandingChargesResponse] =
      cuttlefishClient.getGasStandingCharges(
        GasStandingChargesRequest(
          productCode = productCode,
          tariffCode = tariffCode,
          periodFrom = periodFrom,
          periodTo = periodTo,
          pageSize = pageSize,
        ),
      )
