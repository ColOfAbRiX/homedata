package com.colofabrix.scala.homedata.tado.readings

import cats.kernel.Monoid
import com.colofabrix.scala.homedata.influx.InfluxConfig
import com.colofabrix.scala.tado4s.store.*
import java.time.OffsetDateTime
import scala.concurrent.duration.*

/**
 * Store for Tado Readings
 */
type TadoDataStore =
  TimeSlots[TadoReading]

object TadoDataStore {

  private def TimeResolution: FiniteDuration =
    InfluxConfig.config.timeResolution

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
      TimeSlots.given_Semigroup_TimeSlots[TadoReading].combine(x, y)

}
