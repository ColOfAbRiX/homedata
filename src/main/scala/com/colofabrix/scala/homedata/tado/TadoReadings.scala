package com.colofabrix.scala.homedata.tado

import java.time.Instant
import com.colofabrix.scala.timeflux.measurements.*

final case class TadoReading(
  time: Instant,
  room: String,
  temperature: Double,
  humidity: Double,
  outsideTemperature: Double,
  outsideSun: Double,
)

object TadoReading:

  given InfluxSerializable[TadoReading] with
    override def toMeasurement(reading: TadoReading): Measurement =
      val temperature = MeasurementField("temperature", FieldValue(reading.temperature))
      val humidity    = MeasurementField("humidity", FieldValue(reading.humidity))
      val outsideTemp = MeasurementField("outsideTemperature", FieldValue(reading.outsideTemperature))
      val room        = MeasurementTag("room", reading.room)

      Measurement(
        name = "tado",
        fields = Vector(temperature, humidity, outsideTemp),
        tags = Vector(room),
        time = reading.time,
      )
