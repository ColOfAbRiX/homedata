package com.colofabrix.scala.homedata.tado

import java.time.OffsetDateTime
import com.colofabrix.scala.timeflux.measures.*
import cats.Semigroup

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
      val atHome         = MeasureField("atHome", FieldValue(reading.atHome))
      val windowOpen     = MeasureField("windowOpen", FieldValue(reading.windowOpen))
      val outsideSun     = MeasureField("outsideSun", FieldValue(reading.outsideSun))
      val setTemperature = MeasureField("setTemperature", FieldValue(reading.setTemperature))
      val heating        = MeasureField("heatingModulation", FieldValue(reading.heatingModulation))

      val fields = Vector(temperature, humidity, outsideTemp, atHome, windowOpen, outsideSun, setTemperature, heating)

      Measure("tado", fields, Vector(room), reading.time)
