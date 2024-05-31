package com.colofabrix.scala.homedata.tado.readings

import cats.*
import cats.implicits.given
import com.colofabrix.scala.timeflux.measures.*
import java.time.OffsetDateTime

final case class TadoReading(
  time: OffsetDateTime,
  room: String,
  temperature: Double,
  humidity: Double,
  outsideTemperature: Double,
  setTemperature: Double,
  atHome: Option[Boolean],
  windowOpen: Option[Boolean],
  outsideSun: Option[Boolean],
  heatingModulation: Option[Double],
)

object TadoReading:

  given TimefluxSerializable[TadoReading] with

    override def toMeasure(reading: TadoReading): Measure =
      val room = MeasureTag("room", reading.room)

      val temperature    = MeasureField("temperature", FieldValue(reading.temperature))
      val humidity       = MeasureField("humidity", FieldValue(reading.humidity))
      val outsideTemp    = MeasureField("outsideTemperature", FieldValue(reading.outsideTemperature))
      val setTemperature = MeasureField("setTemperature", FieldValue(reading.setTemperature))

      val atHome     = reading.atHome.map(r => MeasureField("atHome", FieldValue(r))).toVector
      val windowOpen = reading.windowOpen.map(r => MeasureField("windowOpen", FieldValue(r))).toVector
      val outsideSun = reading.outsideSun.map(r => MeasureField("outsideSun", FieldValue(r))).toVector
      val heating    = reading.heatingModulation.map(r => MeasureField("heatingModulation", FieldValue(r))).toVector

      val fields =
        Vector(temperature, humidity, outsideTemp, setTemperature) ++ atHome ++ windowOpen ++ outsideSun ++ heating

      Measure("tado", fields, Vector(room), reading.time)

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
          t.heatingModulation.map(x => s"heatingModulation=$x"),
        )

      fields.flattenOption.mkString("TadoReading(", ", ", ")")
