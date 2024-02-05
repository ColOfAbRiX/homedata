package com.colofabrix.scala.homedata.tado

import cats.*
import cats.implicits.given
import com.colofabrix.scala.homedata.tado.TimeSlots.*
import java.time.*
import java.util.concurrent.TimeUnit
import scala.collection.immutable.TreeMap
import scala.collection.SortedMap
import scala.concurrent.duration.FiniteDuration
import scala.jdk.DurationConverters.*

/**
 * Time Slots accumulator for time series data
 */
final class TimeSlots[A] private (val resolution: FiniteDuration, private val store: InnerStore[A]):

  /**
   * Add a value at a specific point in time into its time slot
   */
  def add(time: OffsetDateTime, value: A): TimeSlots[A] =
    add(time, Vector(value))

  /**
   * Add a collection of values at a specific point in time into its time slot
   */
  def add(time: OffsetDateTime, values: Vector[A]): TimeSlots[A] =
    val slotTime    = roundToTimeSlot(time)
    val timedValues = values.map(TimeValue.InstantValue(time, _))
    val newStore    = addToStore(store, slotTime, timedValues)
    copy(store = newStore)

  /**
   * Adds a value at a specific interval in time into its time slots
   */
  def add(from: OffsetDateTime, to: OffsetDateTime, value: A): TimeSlots[A] =
    add(from, to, Vector(value))

  /**
   * Adds a collection of values at a specific interval in time into its time slots
   */
  def add(from: OffsetDateTime, to: OffsetDateTime, values: Vector[A]): TimeSlots[A] =
    if from === to then
      add(from, values)
    else
      val fromTimeSlot = if from < to then roundToTimeSlot(from) else roundToTimeSlot(to)
      val toTimeSlot   = if from < to then roundToTimeSlot(to) else roundToTimeSlot(from)
      val timedValues  = values.map(TimeValue.TimeSpanValue(from, to, _))
      val newStore     = addRangeToStore(store, resolution, fromTimeSlot, toTimeSlot, timedValues)
      copy(store = newStore)

  /**
   * Non-commutative combine of two TimeSlots
   */
  def combine(other: TimeSlots[A]): TimeSlots[A] =
    if store.isEmpty then
      other
    else if other.store.isEmpty then
      this
    else
      val newStore = combine(store, other.store, resolution)
      copy(store = newStore)

  /**
   * Sets the resolution of the current TimeSlots by upscaling or downscaling the time slots
   */
  def setResolution(newResolution: FiniteDuration): TimeSlots[A] =
    if newResolution === resolution then
      this
    else
      val newStore = combine(InnerStore.empty[A], store, newResolution)
      copy(resolution = newResolution, store = newStore)

  /**
   * Returns a SortedMap of each time slot (even empty ones) for a specific interval of time.
   */
  def toMap(from: OffsetDateTime, to: OffsetDateTime): SortedMap[OffsetDateTime, Vector[TimeValue[A]]] =
    if from === to then
      store
    else
      TimeSlots[A](resolution)
        .add(from, to, Vector.empty[A])
        .combine(this)
        .store

  /**
   * Returns a SortedMap of all time slot (even empty ones)
   */
  def toMap: SortedMap[OffsetDateTime, Vector[TimeValue[A]]] =
    (minDateTime, maxDateTime)
      .mapN(toMap)
      .getOrElse(SortedMap.empty)

  /**
   * Returns a SortedMap of the stored time slots for a specific interval of time.
   */
  def toRawMap(from: OffsetDateTime, to: OffsetDateTime): SortedMap[OffsetDateTime, Vector[TimeValue[A]]] =
    store.filter { case (t, _) => t >= from && t < to }

  /**
   * Returns a SortedMap of all the stored time slots
   */
  val toRawMap: SortedMap[OffsetDateTime, Vector[TimeValue[A]]] =
    store

  /**
   * Time time of the earliest stored time slot
   */
  lazy val minDateTime: Option[OffsetDateTime] =
    store.keySet.minOption

  /**
   * Time time of the earliest stored time slot
   */
  lazy val maxDateTime: Option[OffsetDateTime] =
    store.keySet.maxOption

  /**
   * Indicates if the Time Slots are empty
   */
  lazy val isEmpty: Boolean =
    store.isEmpty

  /**
   * Maps the values of the TimeSlots to a new type
   */
  def map[B](f: A => B): TimeSlots[B] =
    val newStore = store.map { case (t, as) => t -> as.map(_.map(f)) }
    new TimeSlots[B](resolution, newStore)

  //  Java Overrides  //

  override def hashCode(): Int =
    (resolution, store).hashCode()

  override def equals(obj: Any): Boolean =
    obj match
      case other: TimeSlots[_] =>
        resolution === other.resolution && store.equals(other.store)
      case _ =>
        false

  override def toString(): String =
    val prettyStore = store.toString().replaceAll("(^[^\\(]+\\(|\\)$)", "")
    s"TimeSlots(resolution=$resolution, $prettyStore)"

  //  Internals  //

  private def copy(resolution: FiniteDuration = resolution, store: InnerStore[A] = store): TimeSlots[A] =
    new TimeSlots[A](resolution, store)

  private def roundToTimeSlot(value: OffsetDateTime): OffsetDateTime =
    roundToTimeSlot(resolution, value)

  private def roundToTimeSlot(resolution: FiniteDuration, value: OffsetDateTime): OffsetDateTime =
    val resUnit = resolution.unit.toChronoUnit()
    value
      .truncatedTo(resUnit)
      .minus(getDateTimeLength(value, resolution.unit) % resolution.length, resUnit)

  private def addToStore(store: InnerStore[A], slotTime: OffsetDateTime, values: Vector[TimeValue[A]]): InnerStore[A] =
    val newValue = store.get(slotTime).fold(values)(_ ++ values)
    store + (slotTime -> newValue)

  private def addRangeToStore(
    store: InnerStore[A],
    storeResolution: FiniteDuration,
    from: OffsetDateTime,
    to: OffsetDateTime,
    values: Vector[TimeValue[A]],
  ): InnerStore[A] =
    val javaResolution = storeResolution.toJava
    Iterator
      .iterate(from)(_.plus(javaResolution))
      .takeWhile { time =>
        time < to || time === to && from === to
      }
      .foldLeft(store) {
        case (current, time) => addToStore(current, time, values)
      }

  private def combine(target: InnerStore[A], other: InnerStore[A], otherRes: FiniteDuration): InnerStore[A] =
    val roundToOtherTimeSlot = roundToTimeSlot(otherRes, _)

    other.foldLeft(target) {
      case (store1, (_, values)) =>
        values.foldLeft(store1) {
          case (store2, iv @ TimeValue.InstantValue(time, _)) =>
            addToStore(store2, roundToOtherTimeSlot(time), Vector(iv))
          case (store2, tsv @ TimeValue.TimeSpanValue(from, to, _)) =>
            addRangeToStore(store2, otherRes, roundToOtherTimeSlot(from), roundToOtherTimeSlot(to), Vector(tsv))
        }
    }

  private def getDateTimeLength(value: OffsetDateTime, unit: TimeUnit): Long =
    unit match {
      case TimeUnit.DAYS         => value.getDayOfYear
      case TimeUnit.HOURS        => value.getHour
      case TimeUnit.MINUTES      => value.getMinute
      case TimeUnit.SECONDS      => value.getSecond
      case TimeUnit.MILLISECONDS => value.getNano / 1000000
      case TimeUnit.MICROSECONDS => value.getNano / 1000
      case TimeUnit.NANOSECONDS  => value.getNano
    }

object TimeSlots:

  //  TimeValue  //

  enum TimeValue[A]:
    case InstantValue[A](time: OffsetDateTime, value: A)                      extends TimeValue[A]
    case TimeSpanValue[A](from: OffsetDateTime, to: OffsetDateTime, value: A) extends TimeValue[A]

  object TimeValue:

    extension [A](self: TimeValue[A])
      def get: A = self match
        case InstantValue(_, value)     => value
        case TimeSpanValue(_, _, value) => value

    given Functor[TimeValue] with
      def map[A, B](fa: TimeValue[A])(f: A => B): TimeValue[B] =
        fa match {
          case TimeValue.InstantValue(time, value)      => TimeValue.InstantValue(time, f(value))
          case TimeValue.TimeSpanValue(from, to, value) => TimeValue.TimeSpanValue(from, to, f(value))
        }

    given [A: Show]: Show[TimeValue[A]] with
      def show(value: TimeValue[A]): String =
        value match {
          case TimeValue.InstantValue(time, value)      => s"${time}@${value}"
          case TimeValue.TimeSpanValue(from, to, value) => s"${from}~${to}@${value}"
        }

  //  InnerStore  //

  private type InnerStore[A] =
    TreeMap[OffsetDateTime, Vector[TimeValue[A]]]

  private object InnerStore:
    def empty[A]: TreeMap[OffsetDateTime, Vector[TimeValue[A]]] =
      TreeMap.empty[OffsetDateTime, Vector[TimeValue[A]]]

  //  Factory Methods  //

  def apply[A](resolution: FiniteDuration): TimeSlots[A] =
    new TimeSlots(resolution, InnerStore.empty[A])

  def apply[A](resolution: FiniteDuration, time: OffsetDateTime, value: A): TimeSlots[A] =
    new TimeSlots(resolution, InnerStore.empty[A]) add (time, value)

  def apply[A](resolution: FiniteDuration, from: OffsetDateTime, to: OffsetDateTime, value: A): TimeSlots[A] =
    new TimeSlots(resolution, InnerStore.empty[A]).add(from, to, value)

  //  Givens  //

  given [A: Show]: Show[TimeSlots[A]] with
    def show(value: TimeSlots[A]): String =
      val prettyContent = value.store.map { case (time, a) => s"  $time -> ${a.show}" }

      if prettyContent.isEmpty then
        s"${value.getClass.getSimpleName}(resolution=${value.resolution})"
      else
        prettyContent.mkString(s"${value.getClass.getSimpleName}(\n  resolution=${value.resolution},\n", ",\n", "\n)")

  given [A]: Eq[TimeSlots[A]] with
    def eqv(x: TimeSlots[A], y: TimeSlots[A]): Boolean =
      x.equals(y)

  given Functor[TimeSlots] with
    def map[A, B](fa: TimeSlots[A])(f: A => B): TimeSlots[B] =
      fa.map(f)

  given [A]: Semigroup[TimeSlots[A]] with
    def combine(x: TimeSlots[A], y: TimeSlots[A]): TimeSlots[A] =
      x combine y

  //  OffsetDateTime  //

  private given Ordering[OffsetDateTime] =
    Ordering.by(_.toEpochSecond())

  extension (self: OffsetDateTime)
    private def <(other: OffsetDateTime): Boolean   = self.isBefore(other)
    private def <=(other: OffsetDateTime): Boolean  = self.isBefore(other) || self.isEqual(other)
    private def ===(other: OffsetDateTime): Boolean = self.isEqual(other)
    private def >=(other: OffsetDateTime): Boolean  = self.isAfter(other) || self.isEqual(other)
    private def >(other: OffsetDateTime): Boolean   = self.isAfter(other)
