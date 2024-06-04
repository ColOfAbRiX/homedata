package com.colofabrix.scala.homedata.tado.store

import cats.kernel.Monoid
import com.colofabrix.scala.homedata.tado.readings.TadoReading
import java.time.OffsetDateTime
import scala.concurrent.duration.*

/**
  * Store for Tado Readings
  */
type TadoDataStore =
  TimeSlots[TadoReading]

object TadoDataStore:

  private def TimeResolution =
    15.minutes

  def apply(): TadoDataStore =
    TimeSlots[TadoReading](TimeResolution)

  def apply(time: OffsetDateTime, reading: TadoReading): TadoDataStore =
    TimeSlots[TadoReading](TimeResolution, time, reading)

  def apply(from: OffsetDateTime, to: OffsetDateTime, reading: TadoReading): TadoDataStore =
    TimeSlots[TadoReading](TimeResolution, from, to, reading)

  given Monoid[TadoDataStore] with

    def empty: TadoDataStore =
      TadoDataStore()

    def combine(x: TadoDataStore, y: TadoDataStore): TadoDataStore =
      TimeSlots.given_Semigroup_TimeSlots.combine(x, y)
