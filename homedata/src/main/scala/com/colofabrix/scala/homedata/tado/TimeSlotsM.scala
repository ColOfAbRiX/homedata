package com.colofabrix.scala.homedata.tado

import cats.*
import cats.data.Ior
import cats.implicits.given
import cats.kernel.Semigroup
import cats.Monoid
import com.colofabrix.scala.homedata.tado.TimeSlotsM.*
import java.time.*
import java.time.temporal.*
import java.util.concurrent.TimeUnit
import scala.collection.immutable.TreeMap
import scala.collection.SortedMap
import scala.concurrent.duration.FiniteDuration
import scala.jdk.DurationConverters.*

final class TimeSlotsM[A] private (val resolution: FiniteDuration, val store: InnerStore[A]):

  private lazy val javaResolution: Duration =
    resolution.toJava

  def add(time: OffsetDateTime, value: A)(using Semigroup[A]): TimeSlotsM[A] =
    val slotTime = roundToTimeSlot(time)
    val newStore = addToStore(store, slotTime, value)
    copy(store = newStore)

  def addToRange(from: OffsetDateTime, to: OffsetDateTime, value: A)(using Semigroup[A]): TimeSlotsM[A] =
    val fromTimeSlot = roundToTimeSlot(from)
    val toTimeSlot   = roundToTimeSlot(to)
    val newStore     = addRangeToStore(store, fromTimeSlot, toTimeSlot, value)
    copy(store = newStore)

  def combine(other: TimeSlotsM[A])(using Semigroup[A]): TimeSlotsM[A] =
    if store.isEmpty then other
    else if other.store.isEmpty then this
    else
      val newStore = incorporate(store, other.store, other.resolution.toJava)
      copy(store = newStore)

  def changeResolution(newResolution: FiniteDuration)(using Monoid[A]): TimeSlotsM[A] =
    if newResolution === resolution then this
    else
      val newStore = incorporate(InnerStore.empty[A], store, newResolution.toJava)
      copy(store = newStore)

  def toSortedMap(from: OffsetDateTime, to: OffsetDateTime)(using A: Monoid[A]): SortedMap[OffsetDateTime, A] =
    if from isEqual to then
      store
    else
      TimeSlotsM[A](resolution)
        .addToRange(from, to, A.empty)
        .combine(this)
        .store

  def toSortedMap(using A: Monoid[A]): SortedMap[OffsetDateTime, A] =
    (minDateTime, maxDateTime)
      .mapN(toSortedMap)
      .getOrElse(SortedMap.empty)

  def toSlottedSortedMap(from: OffsetDateTime, to: OffsetDateTime): SortedMap[OffsetDateTime, A] =
    store.filter { case (t, _) => t >= from && t < to }

  val toSlottedSortedMap: SortedMap[OffsetDateTime, A] =
    store

  lazy val minDateTime: Option[OffsetDateTime] =
    store.keySet.minOption

  lazy val maxDateTime: Option[OffsetDateTime] =
    store.keySet.maxOption

  override def hashCode(): Int =
    store.hashCode()

  override def equals(x: Any): Boolean =
    store.equals(x)

  //  Internals  //

  private def copy(resolution: FiniteDuration = resolution, store: InnerStore[A] = store): TimeSlotsM[A] =
    new TimeSlotsM[A](resolution, store)

  private def roundToTimeSlot(value: OffsetDateTime): OffsetDateTime =
    val resUnit = resolution.unit.toChronoUnit()
    value
      .truncatedTo(resUnit)
      .minus(getDateTimeLength(value, resolution.unit) % resolution.length, resUnit)

  private def addToStore(store: InnerStore[A], slotTime: OffsetDateTime, value: A)(using A: Semigroup[A]): InnerStore[A] =
    val newValue = store.get(slotTime).fold(value)(_ combine value)
    store + (slotTime -> newValue)

  private def addRangeToStore(
    store: InnerStore[A],
    from: OffsetDateTime,
    to: OffsetDateTime,
    value: A,
  )(using Semigroup[A],
  ): InnerStore[A] =
    Iterator
      .iterate(from)(_.plus(javaResolution))
      .takeWhile { t =>
        t < to || t ==== to && from ==== to
      }
      .foldLeft(store) {
        case (current, time) => addToStore(current, time, value)
      }

  private def incorporate(
    target: InnerStore[A],
    other: InnerStore[A],
    otherRes: Duration,
  )(using Semigroup[A],
  ): InnerStore[A] =
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

object TimeSlotsM:

  private type InnerStore[A] =
    TreeMap[OffsetDateTime, A]

  private object InnerStore:
    def empty[A]: TreeMap[OffsetDateTime, A] =
      TreeMap.empty[OffsetDateTime, A]

  //  Factory Methods  //

  def apply[A: Monoid](resolution: FiniteDuration): TimeSlotsM[A] =
    new TimeSlotsM(resolution, InnerStore.empty[A])

  def apply[A: Monoid](resolution: FiniteDuration, time: OffsetDateTime, value: A): TimeSlotsM[A] =
    new TimeSlotsM(resolution, InnerStore.empty[A]) add (time, value)

  def apply[A: Monoid](resolution: FiniteDuration, from: OffsetDateTime, to: OffsetDateTime, value: A): TimeSlotsM[A] =
    new TimeSlotsM(resolution, InnerStore.empty[A]).addToRange(from, to, value)

  //  Givens  //

  given [A: Show]: Show[TimeSlotsM[A]] with
    def show(value: TimeSlotsM[A]): String =
      val prettyContent =
        value.store.map { case (time, a) => s"  $time -> ${a.show}" }

      if prettyContent.isEmpty then
        s"${value.getClass.getSimpleName}()"
      else
        prettyContent.mkString(s"${value.getClass.getSimpleName}(\n", "\n", "\n)")

  given Functor[TimeSlotsM] with
    def map[A, B](fa: TimeSlotsM[A])(f: A => B): TimeSlotsM[B] =
      val newStore = fa.store.map { case (t, a) => t -> f(a) }
      new TimeSlotsM[B](fa.resolution, newStore)

  given [A: Monoid]: Semigroup[TimeSlotsM[A]] with
    def combine(x: TimeSlotsM[A], y: TimeSlotsM[A]): TimeSlotsM[A] =
      if x.resolution < y.resolution then
        x combine y.changeResolution(x.resolution)
      else
        x.changeResolution(y.resolution) combine y

  //  OffsetDateTime  //

  private given Ordering[OffsetDateTime] =
    Ordering.by(_.toEpochSecond())

  extension (self: OffsetDateTime)
    def <(other: OffsetDateTime): Boolean    = self.isBefore(other)
    def <=(other: OffsetDateTime): Boolean   = self.isBefore(other) || self.isEqual(other)
    def ====(other: OffsetDateTime): Boolean = self.isEqual(other)
    def >=(other: OffsetDateTime): Boolean   = self.isAfter(other) || self.isEqual(other)
    def >(other: OffsetDateTime): Boolean    = self.isAfter(other)
