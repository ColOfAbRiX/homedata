package com.colofabrix.scala.tado4s.api

/** Request to get the active timetable for a zone */
final case class GetActiveTimetableRequest(
  homeId: Int,
  zoneId: Int,
)

/** Request to set the active timetable for a zone */
final case class SetActiveTimetableRequest(
  homeId: Int,
  zoneId: Int,
  timetableId: Int,
)

/** Request to get all timetables for a zone */
final case class GetTimetablesRequest(
  homeId: Int,
  zoneId: Int,
)

/** Request to get timetable blocks */
final case class GetTimetableBlocksRequest(
  homeId: Int,
  zoneId: Int,
  timetableId: Int,
)

/** Request to set timetable blocks for a specific day type */
final case class SetTimetableBlocksRequest(
  homeId: Int,
  zoneId: Int,
  timetableId: Int,
  dayType: String,
  blocks: Vector[TimetableBlocksResponse.Block],
)
