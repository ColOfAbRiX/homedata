package com.colofabrix.scala.homedata.tado

import java.time.OffsetDateTime
import cats.kernel.Monoid
import scala.concurrent.duration.*

type TadoDataStore =
  TimeSlots[TadoRunningReading]

object TadoDataStore:

  private def TimeResolution =
    15.minutes

  def apply(): TadoDataStore =
    TimeSlots[TadoRunningReading](TimeResolution)

  def apply(time: OffsetDateTime, reading: TadoRunningReading): TadoDataStore =
    TimeSlots[TadoRunningReading](TimeResolution, time, reading)

  def apply(from: OffsetDateTime, to: OffsetDateTime, reading: TadoRunningReading): TadoDataStore =
    TimeSlots[TadoRunningReading](TimeResolution, from, to, reading)

  given Monoid[TadoDataStore] with

    def empty: TadoDataStore =
      TadoDataStore()

    def combine(x: TadoDataStore, y: TadoDataStore): TadoDataStore =
      TimeSlots.given_Semigroup_TimeSlots.combine(x, y)
