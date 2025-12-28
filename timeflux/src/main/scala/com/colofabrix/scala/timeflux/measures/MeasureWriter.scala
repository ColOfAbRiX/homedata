package com.colofabrix.scala.timeflux.measures

import cats.implicits.given
import com.colofabrix.scala.timeflux.model.LineProtocolValue
import com.colofabrix.scala.timeflux.api.TimePrecision

/**
 * Serializes a Measure into a Line Protocol value
 *
 * See https://docs.influxdata.com/influxdb/v2/reference/syntax/line-protocol:
 */
private[timeflux] object MeasureWriter {

  def writeToLineProtocol(measure: Measure, timePrecision: TimePrecision): LineProtocolValue =
    val lineMeasure =
      measure
        .name
        .escapeName

    val lineTags =
      measure
        .tags
        .map(t => s"${t.name.escapeTag}=${t.value.escapeTag}")
        .emptyAsNone
        .fold("") {
          _.mkString(",", ",", "")
        }

    val lineFields =
      measure
        .fields
        .map(f => s"${f.name.escapeField}=${writeFieldValue(f.value)}")
        .emptyAsNone
        .fold("") {
          _.mkString(" ", ",", "")
        }

    val lineTime =
      if timePrecision =!= TimePrecision.Milliseconds then
        val timeExponent   = TimePrecision.Milliseconds.multiplier - timePrecision.multiplier
        val timeMultiplier = Math.pow(10, timeExponent.toDouble).toLong
        (measure.time.toInstant.toEpochMilli * timeMultiplier).toString
      else
        measure.time.toInstant.toEpochMilli.toString

    LineProtocolValue(s"${lineMeasure}${lineTags}${lineFields} ${lineTime}".trim)

  extension [A](xs: Vector[A]) {
    private def emptyAsNone: Option[Vector[A]] =
      if xs.isEmpty then None else Some(xs)
  }

  extension (string: String) {
    private def escapeName: String =
      string
        .replaceAll(raw"\\", "\\\\")
        .replaceAll(",", raw"\\,")
        .replaceAll("""\s+""", raw"\\ ")
        .replaceAll("\n", "")
        .trim

    private def escapeTag: String =
      string
        .replaceAll(raw"\\", "\\\\")
        .replaceAll(",", raw"\\,")
        .replaceAll("=", raw"\\=")
        .replaceAll("""\s+""", raw"\\ ")
        .replaceAll("\n", "")
        .trim

    private def escapeField: String =
      string
        .replaceAll(raw"\\", "\\\\")
        .replaceAll(",", raw"\\,")
        .replaceAll("=", raw"\\=")
        .replaceAll("""\s+""", raw"\\ ")
        .replaceAll("\n", "")
        .trim

    private def escapeStringValue: String =
      string
        .replaceAll("\"", "\\\"")
        .trim
  }

  private def writeFieldValue(fieldValue: FieldValue): String =
    fieldValue match
      case FieldValue.StringValue(value)   => s"\"${value.escapeStringValue}\""
      case FieldValue.FloatValue(value)    => value.toString
      case FieldValue.IntegerValue(value)  => s"${value.toString}i"
      case FieldValue.UIntegerValue(value) => s"${value.toString}u"
      case FieldValue.BooleanValue(value)  => value.toString

}
