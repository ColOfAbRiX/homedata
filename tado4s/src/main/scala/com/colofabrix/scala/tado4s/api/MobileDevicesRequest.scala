package com.colofabrix.scala.tado4s.api

/** Request to get mobile devices for a home */
final case class GetMobileDevicesRequest(
  homeId: Int,
)

/** Request to get mobile device settings */
final case class GetMobileDeviceSettingsRequest(
  homeId: Int,
  mobileDeviceId: Int,
)

/** Request to set mobile device settings */
final case class SetMobileDeviceSettingsRequest(
  homeId: Int,
  mobileDeviceId: Int,
  geoTrackingEnabled: Boolean,
  pushNotifications: Option[MobileDeviceSettingsResponse.PushNotifications],
)

/** Request to delete a mobile device */
final case class DeleteMobileDeviceRequest(
  homeId: Int,
  mobileDeviceId: Int,
)
