package com.colofabrix.scala.tado4s.api

import io.circe.Decoder
import io.circe.Encoder

/** Response for early start settings */
final case class EarlyStartResponse(
  enabled: Boolean,
) derives Decoder, Encoder.AsObject
