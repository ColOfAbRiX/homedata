package com.colofabrix.scala.homedata.tado

import cats.*
import cats.data.Ior
import cats.implicits.given
import cats.kernel.Semigroup
import cats.Monoid
import com.colofabrix.scala.homedata.tado.TimeSlotsM.*
import java.time.*
import java.time.temporal.*
import scala.collection.immutable.TreeMap
import scala.collection.SortedMap
import scala.concurrent.duration.FiniteDuration
import scala.jdk.DurationConverters.*
import java.util.concurrent.TimeUnit

final class TimeSlotsM[A] private (val resolution: FiniteDuration, private val store: InnerStore[A]):

  def add(time: OffsetDateTime, value: A)(using Semigroup[A]): TimeSlotsM[A] =
    val slotTime = roundToTimeSlot(time)
    val newStore = addToStore(store, slotTime, value)
    new TimeSlotsM[A](resolution, newStore)

  def combine(other: TimeSlotsM[A])(using Semigroup[A]): TimeSlotsM[A] =
    if store.isEmpty then
      other
    else if other.store.isEmpty then
      this
    else
      val newStore =
        other
          .store
          .foldLeft(store) {
            case (current, (timeSlot, value)) => addToStore(current, timeSlot, value)
          }

      new TimeSlotsM[A](resolution, newStore)

  def addToSlotsRange(from: OffsetDateTime, to: OffsetDateTime, value: A)(using Semigroup[A]): TimeSlotsM[A] =
    val fromTimeSlot = roundToTimeSlot(from)
    val toTimeSlot   = roundToTimeSlot(to)

    val newStore =
      Iterator
        .iterate(fromTimeSlot)(_.plusSeconds(resolution.toSeconds))
        .takeWhile { t =>
          t.isBefore(toTimeSlot) || t.isEqual(toTimeSlot)
        }
        .toVector
        .foldLeft(store) {
          case (current, time) => addToStore(current, time, value)
        }

    new TimeSlotsM[A](resolution, store)

  def changeResolution(newResolution: FiniteDuration)(using Monoid[A]): TimeSlotsM[A] =
    if (newResolution === resolution)
      this
    else
      val initial = new TimeSlotsM[A](newResolution, InnerStore.empty[A])
      store.foldLeft(initial) {
        case (current, (from, value)) =>
          val to = from.plus(resolution.toJava)
          current.addToSlotsRange(from, to, value)
      }

  def toSortedMap(from: OffsetDateTime, to: OffsetDateTime)(using A: Monoid[A]): SortedMap[OffsetDateTime, A] =
    if from isEqual to then
      store
    else
      new TimeSlotsM[A](resolution, InnerStore.empty)
        .addToSlotsRange(from, to, A.empty)
        .combine(this)
        .store

  def toSortedMap()(using A: Monoid[A]): SortedMap[OffsetDateTime, A] =
    (minDateTime, maxDateTime)
      .mapN(toSortedMap)
      .getOrElse(SortedMap.empty)

  lazy val minDateTime: Option[OffsetDateTime] =
    store.keySet.minOption

  lazy val maxDateTime: Option[OffsetDateTime] =
    store.keySet.maxOption

  override def hashCode(): Int =
    store.hashCode()

  override def equals(x: Any): Boolean =
    store.equals(x)

  private def addToStore(store: InnerStore[A], slotTime: OffsetDateTime, value: A)(using A: Semigroup[A]): InnerStore[A] =
    val newValue = store.get(slotTime).fold(value)(_ combine value)
    store + (slotTime -> newValue)

  private def roundToTimeSlot(value: OffsetDateTime): OffsetDateTime =
    val resUnit = resolution.unit.toChronoUnit()
    value
      .truncatedTo(resUnit)
      .minus(getDateTimeLength(value, resolution.unit) % resolution.length, resUnit)

  private def getDateTimeLength(value: OffsetDateTime, unit: TimeUnit): Long =
    unit match {
      case TimeUnit.DAYS         => value.getDayOfYear()
      case TimeUnit.HOURS        => value.getHour
      case TimeUnit.MINUTES      => value.getMinute
      case TimeUnit.SECONDS      => value.getSecond
      case TimeUnit.MILLISECONDS => value.getNano.toLong / 1000000
      case TimeUnit.MICROSECONDS => value.getNano.toLong / 1000
      case TimeUnit.NANOSECONDS  => value.getNano.toLong
    }

object TimeSlotsM:

  private type InnerStore[A] =
    TreeMap[OffsetDateTime, A]

  private object InnerStore:
    def empty[A]: TreeMap[OffsetDateTime, A] =
      TreeMap.empty[OffsetDateTime, A]

  private given Ordering[OffsetDateTime] =
    Ordering.by(_.toEpochSecond())

  //  Factory Methods  //

  def apply[A: Monoid](resolution: FiniteDuration): TimeSlotsM[A] =
    new TimeSlotsM(resolution, InnerStore.empty[A])

  def apply[A: Monoid](resolution: FiniteDuration, time: OffsetDateTime, value: A): TimeSlotsM[A] =
    new TimeSlotsM(resolution, InnerStore.empty[A]) add (time, value)

  def apply[A: Monoid](resolution: FiniteDuration, from: OffsetDateTime, to: OffsetDateTime, value: A): TimeSlotsM[A] =
    new TimeSlotsM(resolution, InnerStore.empty[A]).addToSlotsRange(from, to, value)

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

  given Align[TimeSlotsM] with
    def functor: Functor[TimeSlotsM] =
      summon[Functor[TimeSlotsM]]
    def align[A, B](fa: TimeSlotsM[A], fb: TimeSlotsM[B]): TimeSlotsM[Ior[A, B]] =
      ???
