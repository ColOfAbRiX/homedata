package com.colofabrix.scala.cuttlefish.api

import com.colofabrix.scala.cuttlefish.model.{ ProductCode, TariffCode }
import java.time.OffsetDateTime

/** Request for electricity tariff unit rates endpoints */
final case class ElectricityUnitRatesRequest(
  productCode: ProductCode,
  tariffCode: TariffCode,
  periodFrom: Option[OffsetDateTime],
  periodTo: Option[OffsetDateTime],
  pageSize: Option[Int],
)

/** Request for electricity tariff standing charges endpoint */
final case class ElectricityStandingChargesRequest(
  productCode: ProductCode,
  tariffCode: TariffCode,
  periodFrom: Option[OffsetDateTime],
  periodTo: Option[OffsetDateTime],
  pageSize: Option[Int],
)

/** Request for gas tariff unit rates endpoint */
final case class GasUnitRatesRequest(
  productCode: ProductCode,
  tariffCode: TariffCode,
  periodFrom: Option[OffsetDateTime],
  periodTo: Option[OffsetDateTime],
  pageSize: Option[Int],
)

/** Request for gas tariff standing charges endpoint */
final case class GasStandingChargesRequest(
  productCode: ProductCode,
  tariffCode: TariffCode,
  periodFrom: Option[OffsetDateTime],
  periodTo: Option[OffsetDateTime],
  pageSize: Option[Int],
)
