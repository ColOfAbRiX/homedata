package com.colofabrix.scala.homedata.tado.readings

import cats.*
import cats.implicits.given
import com.colofabrix.scala.timeflux.measures.*
import java.time.OffsetDateTime

final case class TadoReading(
  time: Option[OffsetDateTime] = None,       // DONE
  room: Option[String] = None,               // DONE
  temperature: Option[Double] = None,        // DONE
  humidity: Option[Double] = None,           // DONE
  outsideTemperature: Option[Double] = None, // DONE
  setTemperature: Option[Double] = None,     // DONE
  atHome: Option[Boolean] = None,
  windowOpen: Option[Boolean] = None,
  outsideSun: Option[Boolean] = None,      // DONE
  outsideState: Option[Int] = None,        // DONE
  heatingModulation: Option[Double] = None,// DONE
)

object TadoReading:

  given TimefluxSerializable[TadoReading] with

    override def toMeasure(reading: TadoReading): Measure =
      val room = MeasureTag("room", reading.room.getOrElse("NO-ROOM"))

      val fields =
        reading.temperature.map(r => MeasureField("temperature", FieldValue(r))).toVector ++
        reading.humidity.map(r => MeasureField("humidity", FieldValue(r))).toVector ++
        reading.outsideTemperature.map(r => MeasureField("outsideTemperature", FieldValue(r))).toVector ++
        reading.setTemperature.map(r => MeasureField("setTemperature", FieldValue(r))).toVector ++
        reading.atHome.map(r => MeasureField("atHome", FieldValue(r))).toVector ++
        reading.windowOpen.map(r => MeasureField("windowOpen", FieldValue(r))).toVector ++
        reading.outsideSun.map(r => MeasureField("outsideSun", FieldValue(r))).toVector ++
        reading.outsideState.map(r => MeasureField("outsideState", FieldValue(r))).toVector ++
        reading.heatingModulation.map(r => MeasureField("heatingModulation", FieldValue(r))).toVector

      Measure("tado", fields, Vector(room), reading.time.getOrElse(OffsetDateTime.MAX))

  given Show[TadoReading] with

    def show(t: TadoReading): String =
      val fields =
        List(
          Some(s"time=${t.time}"),
          Some(s"room=${t.room}"),
          Some(s"temperature=${t.temperature}"),
          Some(s"humidity=${t.humidity}"),
          Some(s"setTemperature=${t.setTemperature}"),
          Some(s"outsideTemperature=${t.outsideTemperature}"),
          t.atHome.map(x => s"atHome=$x"),
          t.windowOpen.map(x => s"windowOpen=$x"),
          t.outsideSun.map(x => s"outsideSun=$x"),
          t.outsideState.map(x => s"outsideState=$x"),
          t.heatingModulation.map(x => s"heatingModulation=$x"),
        )

      fields
        .flattenOption
        .mkString("TadoReading(", ", ", ")")

  given Monoid[TadoReading] with
    def empty: TadoReading =
      TadoReading()

    def combine(x: TadoReading, y: TadoReading): TadoReading =
      TadoReading(
        atHome = (x.atHome, y.atHome).last,
        windowOpen = (x.windowOpen, y.windowOpen).last,
        temperature = (x.temperature, y.temperature).avg,
        humidity = (x.humidity, y.humidity).avg,
        outsideTemperature = (x.outsideTemperature, y.outsideTemperature).avg,
        outsideState = (x.outsideState, y.outsideState).last,
        outsideSun = (x.outsideSun, y.outsideSun).last,
        setTemperature = (x.setTemperature, y.setTemperature).avg,
        heatingModulation = (x.heatingModulation, y.heatingModulation).avg,
      )

  extension [A](self: (Option[A], Option[A]))
    def avg(using A: Fractional[A]): Option[A] =
      val list = self._1.toList ::: self._2.toList
      list.reduceOption(A.plus).map(A.div(_, A.fromInt(list.length)))
