package com.colofabrix.scala.homedata.octopus

import java.time.Instant
import com.colofabrix.scala.timeflux.measurements.*

transparent trait OctopusReading

final case class ElectricityReading(time: Instant, consumption: Double) extends OctopusReading

final case class GasReading(time: Instant, consumption: Double) extends OctopusReading

object OctopusReading:

  given InfluxSerializable[OctopusReading] with
    override def toMeasurement(reading: OctopusReading): Measurement =
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
