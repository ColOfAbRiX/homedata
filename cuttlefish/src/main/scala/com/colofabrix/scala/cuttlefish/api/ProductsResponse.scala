package com.colofabrix.scala.cuttlefish.api

import io.circe.Decoder
import java.time.OffsetDateTime

/** Response from the products list endpoint */
final case class ProductsResponse(
  count: Int,
  next: Option[String],
  previous: Option[String],
  results: List[ProductSummary],
) derives Decoder

/** Summary of a product from the products list */
final case class ProductSummary(
  code: String,
  direction: String,
  full_name: String,
  display_name: String,
  description: String,
  is_variable: Boolean,
  is_green: Boolean,
  is_tracker: Boolean,
  is_prepay: Boolean,
  is_business: Boolean,
  is_restricted: Boolean,
  term: Option[Int],
  available_from: Option[OffsetDateTime],
  available_to: Option[OffsetDateTime],
  brand: String,
  links: List[ProductLink],
) derives Decoder

/** Link within a product */
final case class ProductLink(
  href: String,
  method: String,
  rel: String,
) derives Decoder

/** Detailed response from a specific product endpoint */
final case class ProductDetailsResponse(
  code: String,
  full_name: String,
  display_name: String,
  description: String,
  is_variable: Boolean,
  is_green: Boolean,
  is_tracker: Boolean,
  is_prepay: Boolean,
  is_business: Boolean,
  is_restricted: Boolean,
  term: Option[Int],
  available_from: Option[OffsetDateTime],
  available_to: Option[OffsetDateTime],
  tariffs_active_at: Option[OffsetDateTime],
  single_register_electricity_tariffs: Option[Map[String, Map[String, TariffDetails]]],
  dual_register_electricity_tariffs: Option[Map[String, Map[String, TariffDetails]]],
  single_register_gas_tariffs: Option[Map[String, Map[String, TariffDetails]]],
  brand: String,
) derives Decoder

/** Details of a specific tariff */
final case class TariffDetails(
  code: String,
  standing_charge_exc_vat: Option[Double],
  standing_charge_inc_vat: Option[Double],
  online_discount_exc_vat: Option[Double],
  online_discount_inc_vat: Option[Double],
  dual_fuel_discount_exc_vat: Option[Double],
  dual_fuel_discount_inc_vat: Option[Double],
  exit_fees_exc_vat: Option[Double],
  exit_fees_inc_vat: Option[Double],
  exit_fees_type: Option[String],
  standard_unit_rate_exc_vat: Option[Double],
  standard_unit_rate_inc_vat: Option[Double],
  day_unit_rate_exc_vat: Option[Double],
  day_unit_rate_inc_vat: Option[Double],
  night_unit_rate_exc_vat: Option[Double],
  night_unit_rate_inc_vat: Option[Double],
  links: List[ProductLink],
) derives Decoder
