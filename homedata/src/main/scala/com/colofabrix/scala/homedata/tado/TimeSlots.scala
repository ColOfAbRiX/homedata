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
final class TimeSlots[A] private (val resolution: FiniteDuration, val store: InnerStore[A]):

  private lazy val javaResolution: Duration =
    resolution.toJava

  /**
   * Add a value at a specific time into its time slot
   */
  def add(time: OffsetDateTime, value: A): TimeSlots[A] =
    add(time, Vector(value))

  /**
   * Add a collection of values at a specific time into its time slot
   */
  def add(time: OffsetDateTime, values: Vector[A]): TimeSlots[A] =
    val slotTime = roundToTimeSlot(time)
    val newStore = addToStore(store, slotTime, values)
    copy(store = newStore)

  def addToRange(from: OffsetDateTime, to: OffsetDateTime, value: A): TimeSlots[A] =
    addToRange(from, to, Vector(value))

  def addToRange(from: OffsetDateTime, to: OffsetDateTime, values: Vector[A]): TimeSlots[A] =
    val fromTimeSlot = roundToTimeSlot(from)
    val toTimeSlot   = roundToTimeSlot(to)
    val newStore     = addRangeToStore(store, fromTimeSlot, toTimeSlot, values)
    copy(store = newStore)

  def combine(other: TimeSlots[A]): TimeSlots[A] =
    if store.isEmpty then other
    else if other.store.isEmpty then this
    else
      val newStore = incorporate(store, other.store, other.resolution.toJava)
      copy(store = newStore)

  def changeResolution(newResolution: FiniteDuration): TimeSlots[A] =
    if newResolution === resolution then this
    else
      val newStore = incorporate(InnerStore.empty[A], store, newResolution.toJava)
      copy(store = newStore)

  def toMap(from: OffsetDateTime, to: OffsetDateTime): SortedMap[OffsetDateTime, Vector[A]] =
    if from isEqual to then
      store
    else
      TimeSlots[A](resolution)
        .addToRange(from, to, Vector.empty[A])
        .combine(this)
        .store

  def toMap: SortedMap[OffsetDateTime, Vector[A]] =
    (minDateTime, maxDateTime)
      .mapN(toMap)
      .getOrElse(SortedMap.empty)

  def toRawMap(from: OffsetDateTime, to: OffsetDateTime): SortedMap[OffsetDateTime, Vector[A]] =
    store.filter { case (t, _) => t >= from && t < to }

  val toRawMap: SortedMap[OffsetDateTime, Vector[A]] =
    store

  lazy val minDateTime: Option[OffsetDateTime] =
    store.keySet.minOption

  lazy val maxDateTime: Option[OffsetDateTime] =
    store.keySet.maxOption

  override def hashCode(): Int =
    store.hashCode()

  override def equals(obj: Any): Boolean =
    obj match
      case other: TimeSlots[_] =>
        resolution === other.resolution && store.equals(other.store)
      case _ =>
        false

  //  Internals  //

  private def copy(resolution: FiniteDuration = resolution, store: InnerStore[A] = store): TimeSlots[A] =
    new TimeSlots[A](resolution, store)

  private def roundToTimeSlot(value: OffsetDateTime): OffsetDateTime =
    val resUnit = resolution.unit.toChronoUnit()
    value
      .truncatedTo(resUnit)
      .minus(getDateTimeLength(value, resolution.unit) % resolution.length, resUnit)

  private def addToStore(store: InnerStore[A], slotTime: OffsetDateTime, values: Vector[A]): InnerStore[A] =
    val newValue = store.get(slotTime).fold(values)(_ ++ values)
    store + (slotTime -> newValue)

  private def addRangeToStore(
    store: InnerStore[A],
    from: OffsetDateTime,
    to: OffsetDateTime,
    values: Vector[A],
  ): InnerStore[A] =
    Iterator
      .iterate(from)(_.plus(javaResolution))
      .takeWhile { t =>
        t < to || t === to && from === to
      }
      .foldLeft(store) {
        case (current, time) => addToStore(current, time, values)
      }

  private def incorporate(target: InnerStore[A], other: InnerStore[A], otherRes: Duration): InnerStore[A] =
    other.foldLeft(target) {
      case (current, (from, value)) =>
        addRangeToStore(current, roundToTimeSlot(from), from.plus(otherRes), value)
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

  private type InnerStore[A] =
    TreeMap[OffsetDateTime, Vector[A]]

  private object InnerStore:
    def empty[A]: TreeMap[OffsetDateTime, Vector[A]] =
      TreeMap.empty[OffsetDateTime, Vector[A]]

  //  Factory Methods  //

  def apply[A](resolution: FiniteDuration): TimeSlots[A] =
    new TimeSlots(resolution, InnerStore.empty[A])

  def apply[A](resolution: FiniteDuration, time: OffsetDateTime, value: A): TimeSlots[A] =
    new TimeSlots(resolution, InnerStore.empty[A]) add (time, value)

  def apply[A](resolution: FiniteDuration, from: OffsetDateTime, to: OffsetDateTime, value: A): TimeSlots[A] =
    new TimeSlots(resolution, InnerStore.empty[A]).addToRange(from, to, value)

  //  Givens  //

  given [A: Show]: Show[TimeSlots[A]] with
    def show(value: TimeSlots[A]): String =
      val prettyContent = value.store.map { case (time, a) => s"  $time -> ${a.show}" }
      if prettyContent.isEmpty then
        s"${value.getClass.getSimpleName}()"
      else
        prettyContent.mkString(s"${value.getClass.getSimpleName}(\n", "\n", "\n)")

  given [A]: Eq[TimeSlots[A]] with
    def eqv(x: TimeSlots[A], y: TimeSlots[A]): Boolean =
      x.equals(y)

  given Functor[TimeSlots] with
    def map[A, B](fa: TimeSlots[A])(f: A => B): TimeSlots[B] =
      val newStore = fa.store.map { case (t, a) => t -> a.map(f) }
      new TimeSlots[B](fa.resolution, newStore)

  given [A]: Semigroup[TimeSlots[A]] with
    def combine(x: TimeSlots[A], y: TimeSlots[A]): TimeSlots[A] =
      if x.resolution < y.resolution then
        x combine y.changeResolution(x.resolution)
      else
        x.changeResolution(y.resolution) combine y

  //  OffsetDateTime  //

  private given Ordering[OffsetDateTime] =
    Ordering.by(_.toEpochSecond())

  extension (self: OffsetDateTime)
    def <(other: OffsetDateTime): Boolean   = self.isBefore(other)
    def <=(other: OffsetDateTime): Boolean  = self.isBefore(other) || self.isEqual(other)
    def ===(other: OffsetDateTime): Boolean = self.isEqual(other)
    def >=(other: OffsetDateTime): Boolean  = self.isAfter(other) || self.isEqual(other)
    def >(other: OffsetDateTime): Boolean   = self.isAfter(other)
