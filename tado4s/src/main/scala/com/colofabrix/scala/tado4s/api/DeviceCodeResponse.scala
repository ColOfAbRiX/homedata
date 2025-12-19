package com.colofabrix.scala.tado4s.api

import io.circe.Decoder

/**
 * Response from the device code request as defined in RFC 8628
 *
 * @param device_code The device verification code
 * @param user_code The end-user verification code
 * @param verification_uri The URI where the user should enter the user_code
 * @param verification_uri_complete The URI where the user can enter the user_code (may include the code)
 * @param expires_in The lifetime in seconds of the device code and user code
 * @param interval The minimum amount of time in seconds that the client should wait between polling requests
 */
final case class DeviceCodeResponse(
  device_code: String,
  user_code: String,
  verification_uri: String,
  verification_uri_complete: Option[String],
  expires_in: Int,
  interval: Int,
) derives Decoder
