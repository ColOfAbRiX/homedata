package com.colofabrix.scala.homedata.tado

import cats.{ Align, Functor }
import cats.data.Ior
import cats.implicits.given
import cats.Monoid
import TimeSlots.*
import java.time.*
import java.time.temporal.ChronoUnit
import scala.collection.immutable.TreeMap
import scala.collection.SortedMap
import scala.concurrent.duration.FiniteDuration
import TimeSlots

final class TimeSlots[A] private (resolution: FiniteDuration, private val store: InnerStore[A]):

  def +(time: OffsetDateTime, value: A): TimeSlots[A] =
    val slotTime = roundToTimeSlot(time)
    val newStore = addToStore(store, slotTime, value)
    new TimeSlots[A](resolution, newStore)

  def ++(timeSlots: TimeSlots[A]): TimeSlots[A] =
    val newStore =
      timeSlots
        .store
        .foldLeft(store) {
          case (current, (timeSlot, values)) => addToStore(current, timeSlot, values)
        }

    new TimeSlots[A](resolution, newStore)

  def addToSlotRange(from: OffsetDateTime, to: OffsetDateTime, value: A): TimeSlots[A] =
    addToSlotRange(from, to, Vector(value))

  def addToSlotRange(from: OffsetDateTime, to: OffsetDateTime, values: Vector[A]): TimeSlots[A] =
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
          case (current, time) => addToStore(current, time, values)
        }

    new TimeSlots[A](resolution, store)

  def get(time: OffsetDateTime): Option[Vector[A]] =
    store.get(time)

  def map[B](f: A => B): TimeSlots[B] =
    val newStore = store.map { case (t, a) => t -> a.map(f) }
    new TimeSlots[B](resolution, newStore)

  def toSortedMap(from: OffsetDateTime, to: OffsetDateTime): SortedMap[OffsetDateTime, Vector[A]] =
    new TimeSlots[A](resolution, InnerStore.empty)
      .addToSlotRange(from, to, Vector.empty)
      .++(this)
      .store

  def toSortedMap: SortedMap[OffsetDateTime, Vector[A]] =
    toSortedMap(minDateTime, maxDateTime)

  def toMap(from: OffsetDateTime, to: OffsetDateTime): Map[OffsetDateTime, Vector[A]] =
    toSortedMap(from, to).toMap

  def toMap: Map[OffsetDateTime, Vector[A]] =
    toMap(minDateTime, maxDateTime)

  def align[B](other: TimeSlots[B]): Map[OffsetDateTime, Ior[Vector[A], Vector[B]]] =
    (toMap align other.toMap)

  lazy val minDateTime: OffsetDateTime =
    store.keySet.min

  lazy val maxDateTime: OffsetDateTime =
    store.keySet.max

  private def addToStore(store: InnerStore[A], slotTime: OffsetDateTime, value: A): InnerStore[A] =
    addToStore(store, slotTime, Vector(value))

  private def addToStore(store: InnerStore[A], slotTime: OffsetDateTime, values: Vector[A]): InnerStore[A] =
    val slot    = store.getOrElse(slotTime, Vector.empty)
    val newSlot = slot :++ values
    store + (slotTime -> newSlot)

  private def roundToTimeSlot(value: OffsetDateTime): OffsetDateTime =
    val resUnit   = resolution.unit.toChronoUnit()
    val resLength = resolution.length
    value
      .truncatedTo(resUnit)
      .minus(resLength, resUnit)

object TimeSlots:

  private type InnerStore[A] =
    TreeMap[OffsetDateTime, Vector[A]]

  private object InnerStore:
    def empty[A]: TreeMap[OffsetDateTime, Vector[A]] =
      TreeMap.empty[OffsetDateTime, Vector[A]]

  private given Ordering[OffsetDateTime] =
    Ordering.by(_.toEpochSecond())

  def apply[A](resolution: FiniteDuration): TimeSlots[A] =
    new TimeSlots(resolution, InnerStore.empty[A])

  def apply[A](resolution: FiniteDuration, time: OffsetDateTime, value: A): TimeSlots[A] =
    new TimeSlots(resolution, InnerStore.empty[A]) + (time, value)

  def apply[A](resolution: FiniteDuration, from: OffsetDateTime, to: OffsetDateTime, value: A): TimeSlots[A] =
    new TimeSlots(resolution, InnerStore.empty[A]).addToSlotRange(from, to, value)

  given [A]: Functor[TimeSlots] with
    def map[A, B](fa: TimeSlots[A])(f: A => B): TimeSlots[B] = fa.map(f)
