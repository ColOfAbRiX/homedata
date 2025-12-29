package com.colofabrix.scala.tado4s.api

/** Request to get early start settings for a zone */
final case class GetEarlyStartRequest(
  homeId: Int,
  zoneId: Int,
)

/** Request to set early start settings for a zone */
final case class SetEarlyStartRequest(
  homeId: Int,
  zoneId: Int,
  enabled: Boolean,
)
