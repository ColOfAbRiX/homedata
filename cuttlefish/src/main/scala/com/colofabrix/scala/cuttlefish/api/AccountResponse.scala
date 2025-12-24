package com.colofabrix.scala.cuttlefish.api

import io.circe.Decoder
import java.time.OffsetDateTime

/** Response from the account endpoint */
final case class AccountResponse(
  number: String,
  properties: List[Property],
) derives Decoder

/** A property associated with an account */
final case class Property(
  id: Int,
  moved_in_at: Option[OffsetDateTime],
  moved_out_at: Option[OffsetDateTime],
  address_line_1: Option[String],
  address_line_2: Option[String],
  address_line_3: Option[String],
  town: Option[String],
  county: Option[String],
  postcode: Option[String],
  electricity_meter_points: List[ElectricityMeterPoint],
  gas_meter_points: List[GasMeterPoint],
) derives Decoder

/** An electricity meter point */
final case class ElectricityMeterPoint(
  mpan: String,
  profile_class: Option[Int],
  consumption_standard: Option[Int],
  meters: List[Meter],
  agreements: List[Agreement],
) derives Decoder

/** A gas meter point */
final case class GasMeterPoint(
  mprn: String,
  consumption_standard: Option[Int],
  meters: List[Meter],
  agreements: List[Agreement],
) derives Decoder

/** A meter */
final case class Meter(
  serial_number: String,
  registers: List[Register],
) derives Decoder

/** A register on a meter */
final case class Register(
  identifier: String,
  rate: String,
  is_settlement_register: Boolean,
) derives Decoder

/** A tariff agreement */
final case class Agreement(
  tariff_code: String,
  valid_from: OffsetDateTime,
  valid_to: Option[OffsetDateTime],
) derives Decoder
