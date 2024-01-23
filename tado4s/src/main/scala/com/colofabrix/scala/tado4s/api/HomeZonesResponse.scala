package com.colofabrix.scala.tado4s.api

import io.circe.Decoder
import java.time.*

/**
 * HomeZonesResponse
 */
final case class HomeZonesResponse(
  id: Int,
  name: String,
  `type`: String,
  dateCreated: Instant,
  deviceTypes: Vector[String],
  devices: Vector[HomeZonesResponse.Device],
  reportAvailable: Boolean,
  showScheduleSetup: Boolean,
  supportsDazzle: Boolean,
  dazzleEnabled: Boolean,
  dazzleMode: HomeZonesResponse.DazzleMode,
  openWindowDetection: HomeZonesResponse.OpenWindowDetection,
) derives Decoder

object HomeZonesResponse:

  final case class DazzleMode(
    supported: Boolean,
    enabled: Option[Boolean],
  ) derives Decoder

  final case class Device(
    deviceType: String,
    serialNo: String,
    shortSerialNo: String,
    currentFwVersion: String,
    connectionState: ConnectionState,
    characteristics: Characteristics,
    batteryState: Option[String],
    duties: Vector[String],
    mountingState: Option[MountingState],
    mountingStateWithError: Option[String],
    orientation: Option[String],
    childLockEnabled: Option[Boolean],
    isDriverConfigured: Option[Boolean],
  ) derives Decoder

  final case class Characteristics(
    capabilities: Vector[String],
  ) derives Decoder

  final case class ConnectionState(
    value: Boolean,
    timestamp: Instant,
  ) derives Decoder

  final case class MountingState(
    value: String,
    timestamp: Instant,
  ) derives Decoder

  final case class OpenWindowDetection(
    supported: Boolean,
    enabled: Option[Boolean],
    timeoutInSeconds: Option[Int],
  ) derives Decoder
