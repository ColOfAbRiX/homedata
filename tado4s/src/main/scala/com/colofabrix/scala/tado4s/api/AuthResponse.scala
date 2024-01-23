package com.colofabrix.scala.tado4s.api

import io.circe.Decoder

/**
 * AuthResponse
 */
final case class AuthResponse(
  access_token: String,
  token_type: String,
  refresh_token: String,
  expires_in: Int,
  scope: String,
  jti: String,
) derives Decoder
