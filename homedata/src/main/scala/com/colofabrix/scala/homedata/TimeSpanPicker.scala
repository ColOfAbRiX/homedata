package com.colofabrix.scala.homedata

import java.time.*
import java.time.temporal.ChronoUnit
import com.colofabrix.scala.homedata.TimeSpanPicker.Selector

/**
 * TimeSpanPicker makes picking time ranges easier and less painful
 */
final class TimeSpanPicker private (val selector: TimeSpanPicker.Selector, from: OffsetDateTime, to: OffsetDateTime):

  def pick(): (OffsetDateTime, OffsetDateTime) =
    if from.isBefore(to) then (from, to)
    else (to, from)

  def pickFrom(): OffsetDateTime =
    pick()._1

  def pickTo(): OffsetDateTime =
    pick()._2

  def selectFrom(): TimeSpanPicker =
    new TimeSpanPicker(TimeSpanPicker.Selector.From, from, to)

  def selectTo(): TimeSpanPicker =
    new TimeSpanPicker(TimeSpanPicker.Selector.To, from, to)

  def now(): TimeSpanPicker =
    returnUpdatedSelected(OffsetDateTime.now())

  def reset(): TimeSpanPicker =
    TimeSpanPicker()

  def setDateTime(
    year: Int = -1,
    month: Int = -1,
    day: Int = -1,
    hour: Int = -1,
    minute: Int = -1,
    second: Int = -1,
    nanos: Int = -1,
  ): TimeSpanPicker =
    returnUpdatedSelected(
      updateSelected(
        year = if year < 0 then None else Some(year),
        month = if month < 0 then None else Some(month),
        day = if day < 0 then None else Some(day),
        hour = if hour < 0 then None else Some(year),
        minute = if minute < 0 then None else Some(month),
        second = if second < 0 then None else Some(day),
        nanos = if nanos < 0 then None else Some(day),
      ),
    )

  def setDate(year: Int, month: Int, day: Int): TimeSpanPicker =
    returnUpdatedSelected(
      updateSelected(year = Some(year), month = Some(month), day = Some(day)),
    )

  def setTime(hour: Int, minute: Int, second: Int): TimeSpanPicker =
    returnUpdatedSelected(
      updateSelected(hour = Some(hour), minute = Some(minute), second = Some(second)),
    )

  def setZoneOffset(offset: ZoneOffset): TimeSpanPicker =
    returnUpdatedSelected(
      updateSelected(offset = Some(offset)),
    )

  def otherMinus(
    years: Int = 0,
    months: Int = 0,
    weeks: Int = 0,
    days: Int = 0,
    hours: Int = 0,
    minutes: Int = 0,
    seconds: Int = 0,
    nanos: Int = 0,
  ): TimeSpanPicker =
    returnUpdatedSelected(
      getOtherDateTime()
        .minusYears(years)
        .minusMonths(months)
        .minusWeeks(weeks)
        .minusDays(days)
        .minusHours(hours)
        .minusMinutes(minutes)
        .minusSeconds(seconds)
        .minusNanos(nanos),
    )

  def otherPlus(
    years: Int = 0,
    months: Int = 0,
    weeks: Int = 0,
    days: Int = 0,
    hours: Int = 0,
    minutes: Int = 0,
    seconds: Int = 0,
    nanos: Int = 0,
  ): TimeSpanPicker =
    returnUpdatedSelected(
      getOtherDateTime()
        .plusYears(years)
        .plusMonths(months)
        .plusWeeks(weeks)
        .plusDays(days)
        .plusHours(hours)
        .plusMinutes(minutes)
        .plusSeconds(seconds)
        .plusNanos(nanos),
    )

  def roundBoth(at: ChronoUnit): TimeSpanPicker =
    new TimeSpanPicker(selector, from.truncatedTo(at), to.truncatedTo(at))

  private def returnUpdatedSelected(date: OffsetDateTime): TimeSpanPicker =
    selector match
      case Selector.From => new TimeSpanPicker(selector, date, to)
      case Selector.To   => new TimeSpanPicker(selector, from, date)

  private def getSelectedDateTime(): OffsetDateTime =
    selector match
      case Selector.From => from
      case Selector.To   => to

  private def getOtherDateTime(): OffsetDateTime =
    selector match
      case Selector.From => to
      case Selector.To   => from

  private def updateSelected(
    year: Option[Int] = None,
    month: Option[Int] = None,
    day: Option[Int] = None,
    hour: Option[Int] = None,
    minute: Option[Int] = None,
    second: Option[Int] = None,
    nanos: Option[Int] = None,
    offset: Option[ZoneOffset] = None,
  ): OffsetDateTime =
    lazy val selected = getSelectedDateTime()
    OffsetDateTime.of(
      year.getOrElse(selected.getYear()),
      month.getOrElse(selected.getMonthValue()),
      day.getOrElse(selected.getDayOfMonth()),
      hour.getOrElse(selected.getHour()),
      minute.getOrElse(selected.getMinute()),
      second.getOrElse(selected.getSecond()),
      nanos.getOrElse(selected.getNano()),
      offset.getOrElse(selected.getOffset()),
    )

object TimeSpanPicker:

  enum Selector:
    case From
    case To

  def apply(): TimeSpanPicker =
    new TimeSpanPicker(Selector.From, OffsetDateTime.now(), OffsetDateTime.now())
