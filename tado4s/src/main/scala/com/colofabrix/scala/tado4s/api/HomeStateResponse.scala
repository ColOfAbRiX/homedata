package com.colofabrix.scala.tado4s.api

import io.circe.Decoder

/**
 * HomeStateResponse
 */
final case class HomeStateResponse(
  presence: String,
  presenceLocked: Boolean,
) derives Decoder
