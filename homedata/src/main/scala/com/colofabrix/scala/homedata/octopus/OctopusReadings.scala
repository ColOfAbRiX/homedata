package com.colofabrix.scala.homedata.octopus

import java.time.OffsetDateTime
import com.colofabrix.scala.timeflux.measurements.*
import java.time.OffsetDateTime

final case class ElectricityReading(time: OffsetDateTime, consumption: Double)

object ElectricityReading:

  given InfluxSerializable[ElectricityReading] with
    override def toMeasurement(reading: ElectricityReading): Measurement =
      Measurement(
        name = "electricity",
        fields = Vector(MeasurementField("consumption", FieldValue(reading.consumption))),
        tags = Vector.empty,
        time = reading.time,
      )


final case class GasReading(time: OffsetDateTime, consumption: Double)

object GasReading:

  given InfluxSerializable[GasReading] with
    override def toMeasurement(reading: GasReading): Measurement =
      Measurement(
        name = "gas",
        fields = Vector(MeasurementField("consumption", FieldValue(reading.consumption))),
        tags = Vector.empty,
        time = reading.time,
      )
