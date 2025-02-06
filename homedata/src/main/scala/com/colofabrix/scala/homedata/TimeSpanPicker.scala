package com.colofabrix.scala.homedata

import java.time.*
import java.time.temporal.ChronoUnit
import com.colofabrix.scala.homedata.TimeSpanPicker.Selector

/**
 * TimeSpanPicker makes picking time ranges easier and less painful
 */
final class TimeSpanPicker private (val selector: TimeSpanPicker.Selector, from: OffsetDateTime, to: OffsetDateTime):

  /**
   * Returns both From and To dates
   */
  def pick(): (OffsetDateTime, OffsetDateTime) =
    if from.isBefore(to) then (from, to)
    else (to, from)

  /**
   * Returns the From date
   */
  def pickFrom(): OffsetDateTime =
    pick()._1

  /**
   * Returns the To date
   */
  def pickTo(): OffsetDateTime =
    pick()._2

  /**
   * Focuses the TimeSpanPicker to the From date
   */
  def selectFrom(): TimeSpanPicker =
    new TimeSpanPicker(TimeSpanPicker.Selector.From, from, to)

  /**
   * Focuses the TimeSpanPicker to the To date
   */
  def selectTo(): TimeSpanPicker =
    new TimeSpanPicker(TimeSpanPicker.Selector.To, from, to)

  /**
   * Sets the TimeSpanPicker to now
   */
  def now(): TimeSpanPicker =
    returnUpdatedSelected(OffsetDateTime.now())

  /**
   * Resets the TimeSpanPicker configuration
   */
  def reset(): TimeSpanPicker =
    TimeSpanPicker()

  /**
   * Sets individual parts of the Date and Time of the selected From/To date
   */
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

  /**
   * Sets individual parts of the Date of the selected From/To date
   */
  def setDate(year: Int, month: Int, day: Int): TimeSpanPicker =
    returnUpdatedSelected(
      updateSelected(year = Some(year), month = Some(month), day = Some(day)),
    )

  /**
   * Sets individual parts of the Time of the selected From/To date
   */
  def setTime(hour: Int, minute: Int, second: Int): TimeSpanPicker =
    returnUpdatedSelected(
      updateSelected(hour = Some(hour), minute = Some(minute), second = Some(second)),
    )

  /**
   * Sets individual Zone Offset of the selected From/To date
   */
  def setZoneOffset(offset: ZoneOffset): TimeSpanPicker =
    returnUpdatedSelected(
      updateSelected(offset = Some(offset)),
    )

  /**
   * Subtracts a given amount of time from the non-selected From/To date
   */
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

  /**
   * Adds a given amount of time from the non-selected From/To date
   */
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

  /**
   * Rounds both From and To dates to the given Chrono Unit
   */
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

  private[TimeSpanPicker] enum Selector:
    case From
    case To

  /**
   * Creates a new TimeSpanPicker set to Now
   */
  def apply(): TimeSpanPicker =
    new TimeSpanPicker(Selector.From, OffsetDateTime.now(), OffsetDateTime.now())
