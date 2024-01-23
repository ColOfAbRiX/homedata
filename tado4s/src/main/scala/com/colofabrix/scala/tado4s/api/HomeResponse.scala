package com.colofabrix.scala.tado4s.api

import io.circe.Decoder
import java.time.*

/**
 * HomeResponse
 */
final case class HomeResponse(
  id: Int,
  name: String,
  dateTimeZone: String, // Could be ZoneId
  dateCreated: Instant,
  temperatureUnit: String,
  partner: Option[String],
  simpleSmartScheduleEnabled: Boolean,
  awayRadiusInMeters: Double,
  installationCompleted: Boolean,
  incidentDetection: HomeResponse.IncidentDetection,
  generation: String,
  zonesCount: Int,
  skills: Vector[String],
  christmasModeEnabled: Boolean,
  showAutoAssistReminders: Boolean,
  contactDetails: HomeResponse.ContactDetails,
  address: HomeResponse.Address,
  geolocation: HomeResponse.Geolocation,
  consentGrantSkippable: Boolean,
  enabledFeatures: Vector[String],
  isAirComfortEligible: Boolean,
  isBalanceAcEligible: Boolean,
  isBalanceHpEligible: Boolean,
  isEnergyIqEligible: Boolean,
  isHeatSourceInstalled: Boolean,
) derives Decoder

object HomeResponse:

  final case class Address(
    addressLine1: String,
    addressLine2: Option[String],
    zipCode: String,
    city: String,
    state: Option[String],
    country: String,
  ) derives Decoder

  final case class ContactDetails(
    name: String,
    email: String,
    phone: String,
  ) derives Decoder

  final case class Geolocation(
    latitude: Double,
    longitude: Double,
  ) derives Decoder

  final case class IncidentDetection(
    supported: Boolean,
    enabled: Boolean,
  ) derives Decoder
