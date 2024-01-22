package com.colofabrix.scala.timeflux.model

import java.time.Instant

final case class Measurement(
  name: String,
  fields: Vector[MeasurementField],
  tags: Vector[MeasurementTag],
  time: Instant,
)

final case class MeasurementField(name: String, value: FieldValue)

enum FieldValue:
  case MString(value: String)    extends FieldValue
  case MFloat(value: BigDecimal) extends FieldValue
  case MInteger(value: Int)      extends FieldValue
  case MUInteger(value: Long)    extends FieldValue
  case MBoolean(value: Boolean)  extends FieldValue

object FieldValue:

  def apply[A](value: A): FieldValue =
    value match
      case v: String     => FieldValue.MString(v)
      case v: BigDecimal => FieldValue.MFloat(v)
      case v: Double     => FieldValue.MFloat(v)
      case v: Int        => FieldValue.MInteger(v)
      case v: Long       => FieldValue.MUInteger(v)
      case v: Boolean    => FieldValue.MBoolean(v)
      case v: Any        => FieldValue.MString(v.toString())

final case class MeasurementTag(name: String, value: String)
