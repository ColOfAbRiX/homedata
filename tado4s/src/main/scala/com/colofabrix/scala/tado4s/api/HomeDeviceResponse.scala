package com.colofabrix.scala.tado4s.api

import enumeratum.*
import enumeratum.EnumEntry.UpperSnakecase
import io.circe.*
import io.circe.derivation.Configuration

/**
 * HomeDeviceResponse
 */
final case class HomeDeviceResponse (
  deviceType: String,
  serialNo: String,
  shortSerialNo: String,
  currentFwVersion: String,
  connectionState: ConnectionState,
  characteristics: Characteristics,
  isDriverConfigured: Option[Boolean],
  inPairingMode: Option[Boolean],
  batteryState: Option[String],
  mountingState: Option[MountingState],
  mountingStateWithError: Option[String],
  orientation: Option[DeviceOrientation],
  childLockEnabled: Option[Boolean]
) derives Decoder

final case class Characteristics(
  capabilities: Vector[String],
) derives Decoder

final case class ConnectionState(
  value: Boolean,
  timestamp: String,
) derives Decoder

final case class MountingState(
  value: String,
  timestamp: String,
) derives Decoder

sealed trait DeviceOrientation extends EnumEntry with UpperSnakecase

object DeviceOrientation extends Enum[DeviceOrientation] with CirceEnum[DeviceOrientation]:

  case object Horizontal extends DeviceOrientation
  case object Vertical   extends DeviceOrientation

  val values = findValues
