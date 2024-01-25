package com.colofabrix.scala.homedata.tado

import java.time.Instant
import com.colofabrix.scala.timeflux.measurements.*

final case class TadoReading(
  time: Instant,
  room: String,
  atHome: Boolean,
  windowOpen: Boolean,
  temperature: Double,
  humidity: Double,
  outsideTemperature: Double,
  outsideSun: Double,
  setTemperature: Double,
  heatingModulation: Double,
)

object TadoReading:

  given InfluxSerializable[TadoReading] with
    override def toMeasurement(reading: TadoReading): Measurement =
      val room = MeasurementTag("room", reading.room)

      val temperature    = MeasurementField("temperature", FieldValue(reading.temperature))
      val humidity       = MeasurementField("humidity", FieldValue(reading.humidity))
      val outsideTemp    = MeasurementField("outsideTemperature", FieldValue(reading.outsideTemperature))
      val atHome         = MeasurementField("atHome", FieldValue(reading.atHome))
      val windowOpen     = MeasurementField("windowOpen", FieldValue(reading.windowOpen))
      val outsideSun     = MeasurementField("outsideSun", FieldValue(reading.outsideSun))
      val setTemperature = MeasurementField("setTemperature", FieldValue(reading.setTemperature))
      val heating        = MeasurementField("heatingModulation", FieldValue(reading.heatingModulation))

      val fields = Vector(temperature, humidity, outsideTemp, atHome, windowOpen, outsideSun, setTemperature, heating)

      Measurement("tado", fields, Vector(room), reading.time)
