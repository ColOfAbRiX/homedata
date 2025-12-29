package com.colofabrix.scala.tado4s.api

import io.circe.Encoder

/** Request to set home presence */
final case class SetHomePresenceRequest(
  homeId: Int,
  homePresence: String,
) derives Encoder.AsObject
