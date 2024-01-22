package com.colofabrix.scala.homedata.octopus

import java.time.Instant
import com.colofabrix.scala.timeflux.model.*
import com.colofabrix.scala.timeflux.InfluxSerializable

transparent trait Reading

final case class ElectricityReading(time: Instant, consumption: Double) extends Reading

final case class GasReading(time: Instant, consumption: Double) extends Reading

object Reading:

  given InfluxSerializable[Reading] with
    override def toMeasurement(reading: Reading): Measurement =
      reading match
        case er: ElectricityReading => er.toMeasurement
        case gr: GasReading         => gr.toMeasurement

  given InfluxSerializable[ElectricityReading] with
    override def toMeasurement(reading: ElectricityReading): Measurement =
      Measurement(
        name = "electricity",
        fields = Vector(MeasurementField("consumption", FieldValue(reading.consumption))),
        tags = Vector.empty,
        time = reading.time,
      )

  given InfluxSerializable[GasReading] with
    override def toMeasurement(reading: GasReading): Measurement =
      Measurement(
        name = "gas",
        fields = Vector(MeasurementField("consumption", FieldValue(reading.consumption))),
        tags = Vector.empty,
        time = reading.time,
      )
