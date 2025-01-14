package com.colofabrix.scala.tado4s.api

import io.circe.*

/**
 * HomeInstallationResponse
 *
 * NOTE: I could not find any sample data so this structure is untested
 */
final case class HomeInstallationResponse (
  id: Int,
  `type`: String,
  revision: Int,
  state: String,
  devices: Vector[Device]
) derives Decoder

final case class Device (
  deviceType: String,
  serialNo: String,
  shortSerialNo: String,
  currentFwVersion: String,
  connectionState: ConnectionState,
  characteristics: Characteristics
) derives Decoder
