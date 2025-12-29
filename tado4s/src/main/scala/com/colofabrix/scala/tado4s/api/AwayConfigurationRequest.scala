package com.colofabrix.scala.tado4s.api

/** Request to get away configuration for a zone */
final case class GetAwayConfigurationRequest(
  homeId: Int,
  zoneId: Int,
)

/** Request to set away configuration for a zone */
final case class SetAwayConfigurationRequest(
  homeId: Int,
  zoneId: Int,
  `type`: String,
  autoAdjust: Boolean,
  comfortLevel: Int,
  setting: AwayConfigurationResponse.Setting,
)
