package com.colofabrix.scala.homedata.tado

import cats.{ Align, Functor }
import cats.data.Ior
import cats.implicits.given
import cats.Monoid
import com.colofabrix.scala.homedata.tado.TimeSlotsM.*
import java.time.*
import java.time.temporal.ChronoUnit
import scala.collection.immutable.TreeMap
import scala.collection.SortedMap
import scala.concurrent.duration.FiniteDuration

final class TimeSlotsM[A] private (val resolution: FiniteDuration, private val store: InnerStore[A]):

  def +(time: OffsetDateTime, value: A)(using Monoid[A]): TimeSlotsM[A] =
    val slotTime = roundToTimeSlot(time)
    val newStore = addToStore(store, slotTime, value)
    new TimeSlotsM[A](resolution, newStore)

  def ++(timeSlots: TimeSlotsM[A])(using Monoid[A]): TimeSlotsM[A] =
    val newStore =
      timeSlots
        .store
        .foldLeft(store) {
          case (current, (timeSlot, value)) => addToStore(current, timeSlot, value)
        }

    new TimeSlotsM[A](resolution, newStore)

  def addToSlotRange(from: OffsetDateTime, to: OffsetDateTime, value: A)(using Monoid[A]): TimeSlotsM[A] =
    val fromTimeSlot = roundToTimeSlot(from)
    val toTimeSlot   = roundToTimeSlot(to)

    val newStore =
      Iterator
        .iterate(fromTimeSlot)(_.plusSeconds(resolution.toSeconds))
        .toVector
        .takeWhile { t =>
          t.isBefore(toTimeSlot) || t.isEqual(toTimeSlot)
        }
        .foldLeft(store) {
          case (current, time) => addToStore(current, time, value)
        }

    new TimeSlotsM[A](resolution, store)

  def changeResolution(newResolution: FiniteDuration)(using Monoid[A]): TimeSlotsM[A] =
    if (newResolution < resolution)
      increaseRes(newResolution)
    else if (newResolution == resolution)
      this
    else
      decreaseRes(newResolution)

  private def increaseRes(newRes: FiniteDuration)(using Monoid[A]): TimeSlotsM[A] =
    store.foldLeft(new TimeSlotsM[A](newRes, InnerStore.empty[A])) {
      case (current, (from, value)) =>
        val to = from.plus(resolution.length, resolution.unit.toChronoUnit())
        current.addToSlotRange(from, to, value)
    }

  private def decreaseRes(newRes: FiniteDuration)(using Monoid[A]): TimeSlotsM[A] =
    ???

  def toSortedMap(from: OffsetDateTime, to: OffsetDateTime)(using A: Monoid[A]): SortedMap[OffsetDateTime, A] =
    new TimeSlotsM[A](resolution, InnerStore.empty)
      .addToSlotRange(from, to, A.empty)
      .++(this)
      .store

  def toMap(from: OffsetDateTime, to: OffsetDateTime)(using Monoid[A]): Map[OffsetDateTime, A] =
    toSortedMap(from, to).toMap

  def toMap(using Monoid[A]): Map[OffsetDateTime, A] =
    toMap(minDateTime, maxDateTime)

  lazy val minDateTime: OffsetDateTime =
    store.keySet.min

  lazy val maxDateTime: OffsetDateTime =
    store.keySet.max

  private def addToStore(store: InnerStore[A], slotTime: OffsetDateTime, value: A)(using A: Monoid[A]): InnerStore[A] =
    val newValue = store.get(slotTime).fold(value)(_ combine value)
    store + (slotTime -> newValue)

  private def roundToTimeSlot(value: OffsetDateTime): OffsetDateTime =
    val resUnit   = resolution.unit.toChronoUnit()
    val resLength = resolution.length
    value
      .truncatedTo(resUnit)
      .minus(resLength, resUnit)

object TimeSlotsM:

  private type InnerStore[A] =
    TreeMap[OffsetDateTime, A]

  private object InnerStore:
    def empty[A]: TreeMap[OffsetDateTime, A] =
      TreeMap.empty[OffsetDateTime, A]

  private given Ordering[OffsetDateTime] =
    Ordering.by(_.toEpochSecond())

  def apply[A: Monoid](resolution: FiniteDuration): TimeSlotsM[A] =
    new TimeSlotsM(resolution, InnerStore.empty[A])

  def apply[A: Monoid](resolution: FiniteDuration, time: OffsetDateTime, value: A): TimeSlotsM[A] =
    new TimeSlotsM(resolution, InnerStore.empty[A]) + (time, value)

  def apply[A: Monoid](resolution: FiniteDuration, from: OffsetDateTime, to: OffsetDateTime, value: A): TimeSlotsM[A] =
    new TimeSlotsM(resolution, InnerStore.empty[A]).addToSlotRange(from, to, value)

  given Functor[TimeSlotsM] with
    def map[A, B](fa: TimeSlotsM[A])(f: A => B): TimeSlotsM[B] =
      val newStore = fa.store.map { case (t, a) => t -> f(a) }
      new TimeSlotsM[B](fa.resolution, newStore)

  given [A: Monoid]: Monoid[TimeSlotsM[A]] with
    def empty: TimeSlotsM[A] =
      ???
    def combine(x: TimeSlotsM[A], y: TimeSlotsM[A]): TimeSlotsM[A] =
      if x.resolution < y.resolution then
        x ++ y.changeResolution(x.resolution)
      else
        x.changeResolution(y.resolution) ++ y

  given Align[TimeSlotsM] with
    def functor: Functor[TimeSlotsM] =
      summon[Functor[TimeSlotsM]]
    def align[A, B](fa: TimeSlotsM[A], fb: TimeSlotsM[B]): TimeSlotsM[Ior[A, B]] =
      ???
