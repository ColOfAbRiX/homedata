package com.colofabrix.scala.timeflux.measures

import java.time.OffsetDateTime

/**
 * InfluxDB Measure
 */
final case class Measure(
  name: String,
  fields: Vector[MeasureField],
  tags: Vector[MeasureTag],
  time: OffsetDateTime
)

object Measure {

  given TimefluxSerializable[Measure] with
    def toMeasure(m: Measure): Measure = m

}

/**
 * Measure Field
 */
final case class MeasureField(name: String, value: FieldValue)

enum FieldValue {
  case StringValue(value: String)    extends FieldValue
  case FloatValue(value: BigDecimal) extends FieldValue
  case IntegerValue(value: Int)      extends FieldValue
  case UIntegerValue(value: Long)    extends FieldValue
  case BooleanValue(value: Boolean)  extends FieldValue
}

object FieldValue {

  def apply[A](value: A): FieldValue =
    value match
      case v: String     => FieldValue.StringValue(v)
      case v: BigDecimal => FieldValue.FloatValue(v)
      case v: Double     => FieldValue.FloatValue(v)
      case v: Int        => FieldValue.IntegerValue(v)
      case v: Long       => FieldValue.UIntegerValue(v)
      case v: Boolean    => FieldValue.BooleanValue(v)
      case v: Any        => FieldValue.StringValue(v.toString())

}

/**
 * Measure Tag
 */
final case class MeasureTag(name: String, value: String)
