package com.colofabrix.scala.homedata.octopus

import com.colofabrix.scala.timeflux.measures.*
import java.time.OffsetDateTime
import com.colofabrix.scala.timeflux.api.TimePrecision

sealed trait OctopusReading

object OctopusReading:

  given TimefluxSerializable[OctopusReading] with
    override def toMeasure(reading: OctopusReading): Measure =
      reading match {
        case er: ElectricityReading => er.toMeasure
        case gr: GasReading         => gr.toMeasure
      }

final case class ElectricityReading(time: OffsetDateTime, consumption: Double) extends OctopusReading

object ElectricityReading:

  given TimefluxSerializable[ElectricityReading] with
    override def toMeasure(reading: ElectricityReading): Measure =
      Measure(
        name = "electricity",
        fields = Vector(MeasureField("consumption", FieldValue(reading.consumption))),
        tags = Vector.empty,
        time = reading.time,
        precision = TimePrecision.Milliseconds
      )

final case class GasReading(time: OffsetDateTime, consumption: Double) extends OctopusReading

object GasReading:

  given TimefluxSerializable[GasReading] with
    override def toMeasure(reading: GasReading): Measure =
      Measure(
        name = "gas",
        fields = Vector(MeasureField("consumption", FieldValue(reading.consumption))),
        tags = Vector.empty,
        time = reading.time,
        precision = TimePrecision.Milliseconds
      )
