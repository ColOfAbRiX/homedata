package com.colofabrix.scala.timeflux.measurements

import java.time.OffsetDateTime

/**
 * InfluxDB Measurement
 */
final case class Measurement(
  name: String,
  fields: Vector[MeasurementField],
  tags: Vector[MeasurementTag],
  time: OffsetDateTime,
)

object Measurement:

  given InfluxSerializable[Measurement] with
    def toMeasurement(m: Measurement): Measurement = m

/**
 * Measurement Field
 */
final case class MeasurementField(name: String, value: FieldValue)

enum FieldValue:
  case StringValue(value: String)    extends FieldValue
  case FloatValue(value: BigDecimal) extends FieldValue
  case IntegerValue(value: Int)      extends FieldValue
  case UIntegerValue(value: Long)    extends FieldValue
  case BooleanValue(value: Boolean)  extends FieldValue

object FieldValue:

  def apply[A](value: A): FieldValue =
    value match
      case v: String     => FieldValue.StringValue(v)
      case v: BigDecimal => FieldValue.FloatValue(v)
      case v: Double     => FieldValue.FloatValue(v)
      case v: Int        => FieldValue.IntegerValue(v)
      case v: Long       => FieldValue.UIntegerValue(v)
      case v: Boolean    => FieldValue.BooleanValue(v)
      case v: Any        => FieldValue.StringValue(v.toString())

/**
 * Measurement Tag
 */
final case class MeasurementTag(name: String, value: String)
