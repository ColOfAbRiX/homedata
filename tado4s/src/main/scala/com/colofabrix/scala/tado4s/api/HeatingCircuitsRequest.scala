package com.colofabrix.scala.tado4s.api

/** Request to get heating circuits for a home */
final case class GetHeatingCircuitsRequest(
  homeId: Int,
)
