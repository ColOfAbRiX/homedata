package com.colofabrix.scala.tado4s.api

/** Request for zone capabilities */
final case class ZoneCapabilitiesRequest(
  homeId: Int,
  zoneId: Int,
)
