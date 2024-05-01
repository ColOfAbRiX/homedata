package com.colofabrix.scala.timeflux.measures

import com.colofabrix.scala.timeflux.model.LineProtocolValue

/**
  * Serializes a Measure into a Line Protocol value
  */
private[timeflux] object MeasureWriter:

  def writeToLineProtocol(measure: Measure): LineProtocolValue =
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
          _.mkString("", ",", " ")
        }

    val lineFields =
      measure
        .fields
        .map(f => s"${f.name.escapeField}=${writeFieldValue(f.value)}")
        .emptyAsNone
        .fold("") {
          _.mkString("", ",", " ")
        }

    val lineTime =
      measure
        .time
        .toInstant
        .toEpochMilli
        .toString

    LineProtocolValue(s"${lineMeasure} ${lineTags}${lineFields}${lineTime}\n")

  extension [A](xs: Vector[A])
    private def emptyAsNone: Option[Vector[A]] = if xs.isEmpty then None else Some(xs)

  extension (string: String)
    private def escapeName: String =
      string
        .replaceAll("\\,", "\\,")
        .trim()

    private def escapeTag: String =
      string
        .replaceAll("\\s+", "\\ ")
        .replaceAll("=", "\\=")
        .replaceAll("\\,", "\\,")
        .trim()

    private def escapeField: String =
      string
        .replaceAll("\\s+", "\\ ")
        .replaceAll("\\,", "\\,")
        .trim()

    private def escapeStringValue: String =
      string
        .replaceAll("\"", "\\\"")
        .trim()

  private def writeFieldValue(fieldValue: FieldValue): String =
    fieldValue match
      case FieldValue.StringValue(value)   => s"\"${value.escapeStringValue}\""
      case FieldValue.FloatValue(value)    => value.toString
      case FieldValue.IntegerValue(value)  => value.toString
      case FieldValue.UIntegerValue(value) => value.toString
      case FieldValue.BooleanValue(value)  => value.toString
