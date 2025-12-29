package com.colofabrix.scala.cuttlefish.api

import io.circe.Decoder
import java.time.OffsetDateTime

/** Response from the unit rates endpoint */
final case class UnitRatesResponse(
  count: Int,
  next: Option[String],
  previous: Option[String],
  results: List[UnitRate],
) derives Decoder

/** A unit rate from the tariff */
final case class UnitRate(
  value_exc_vat: Double,
  value_inc_vat: Double,
  valid_from: OffsetDateTime,
  valid_to: Option[OffsetDateTime],
  payment_method: Option[String],
) derives Decoder

/** Response from the standing charges endpoint */
final case class StandingChargesResponse(
  count: Int,
  next: Option[String],
  previous: Option[String],
  results: List[StandingCharge],
) derives Decoder

/** A standing charge from the tariff */
final case class StandingCharge(
  value_exc_vat: Double,
  value_inc_vat: Double,
  valid_from: OffsetDateTime,
  valid_to: Option[OffsetDateTime],
  payment_method: Option[String],
) derives Decoder
