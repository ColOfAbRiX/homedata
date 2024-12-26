package com.colofabrix.scala.homedata.tado.readings

import cats.*
import cats.implicits.given
import com.colofabrix.scala.timeflux.measures.*
import java.time.OffsetDateTime

final case class TadoReading(
  time: Option[OffsetDateTime],
  room: Option[String],
  temperature: Option[Double],
  humidity: Option[Double],
  outsideTemperature: Option[Double],
  setTemperature: Option[Double],
  atHome: Option[Boolean],
  windowOpen: Option[Boolean],
  manualSet: Option[Boolean],
  outsideSun: Option[Boolean],
  outsideState: Option[Int],
  heatingModulation: Option[Double],
)

object TadoReading:

  def build(
    time: Option[OffsetDateTime] = None,
    room: Option[String] = None,
    temperature: Option[Double] = None,
    humidity: Option[Double] = None,
    outsideTemperature: Option[Double] = None,
    setTemperature: Option[Double] = None,
    atHome: Option[Boolean] = None,
    windowOpen: Option[Boolean] = None,
    manualSet: Option[Boolean] = None,
    outsideSun: Option[Boolean] = None,
    outsideState: Option[Int] = None,
    heatingModulation: Option[Double] = None,
  ) = TadoReading(
    time,
    room,
    temperature,
    humidity,
    outsideTemperature,
    setTemperature,
    atHome,
    windowOpen,
    manualSet,
    outsideSun,
    outsideState,
    heatingModulation,
  )

  given TimefluxSerializable[TadoReading] with

    override def toMeasure(reading: TadoReading): Measure =
      val room = MeasureTag("room", reading.room.getOrElse("NO-ROOM"))

      val fields =
        reading.atHome.map(r => MeasureField("atHome", FieldValue(r))).toVector ++
        reading.heatingModulation.map(r => MeasureField("heatingModulation", FieldValue(r))).toVector ++
        reading.humidity.map(r => MeasureField("humidity", FieldValue(r))).toVector ++
        reading.manualSet.map(r => MeasureField("manualSet", FieldValue(r))).toVector ++
        reading.outsideState.map(r => MeasureField("outsideState", FieldValue(r))).toVector ++
        reading.outsideSun.map(r => MeasureField("outsideSun", FieldValue(r))).toVector ++
        reading.outsideTemperature.map(r => MeasureField("outsideTemperature", FieldValue(r))).toVector ++
        reading.setTemperature.map(r => MeasureField("setTemperature", FieldValue(r))).toVector ++
        reading.temperature.map(r => MeasureField("temperature", FieldValue(r))).toVector ++
        reading.windowOpen.map(r => MeasureField("windowOpen", FieldValue(r))).toVector

      Measure("tado", fields, Vector(room), reading.time.getOrElse(OffsetDateTime.MAX))

  given Show[TadoReading] with

    def show(t: TadoReading): String =
      val fields =
        List(
          Some(s"humidity=${t.humidity}"),
          Some(s"outsideTemperature=${t.outsideTemperature}"),
          Some(s"room=${t.room}"),
          Some(s"setTemperature=${t.setTemperature}"),
          Some(s"temperature=${t.temperature}"),
          Some(s"time=${t.time}"),
          t.atHome.map(x => s"atHome=$x"),
          t.heatingModulation.map(x => s"heatingModulation=$x"),
          t.manualSet.map(x => s"manualSet=$x"),
          t.outsideState.map(x => s"outsideState=$x"),
          t.outsideSun.map(x => s"outsideSun=$x"),
          t.windowOpen.map(x => s"windowOpen=$x"),
        )

      fields
        .flattenOption
        .mkString("TadoReading(", ", ", ")")

  given Monoid[TadoReading] with
    def empty: TadoReading =
      TadoReading.build()

    def combine(x: TadoReading, y: TadoReading): TadoReading =
      TadoReading.build(
        atHome = (x.atHome, y.atHome).last,
        heatingModulation = (x.heatingModulation, y.heatingModulation).avg,
        humidity = (x.humidity, y.humidity).avg,
        manualSet = (x.manualSet, y.manualSet).last,
        outsideState = (x.outsideState, y.outsideState).last,
        outsideSun = (x.outsideSun, y.outsideSun).last,
        outsideTemperature = (x.outsideTemperature, y.outsideTemperature).avg,
        setTemperature = (x.setTemperature, y.setTemperature).avg,
        temperature = (x.temperature, y.temperature).avg,
        windowOpen = (x.windowOpen, y.windowOpen).last,
      )

  extension [A](self: (Option[A], Option[A]))
    def avg(using A: Fractional[A]): Option[A] =
      val list = self._1.toList ::: self._2.toList
      list.reduceOption(A.plus).map(A.div(_, A.fromInt(list.length)))
