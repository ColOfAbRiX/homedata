package com.colofabrix.scala.timeflux

import com.colofabrix.scala.timeflux.model.*

private[timeflux] object MeasurementWriter:

  def writeToLineProtocol(measurement: Measurement): String =
    val lineMeasurement = escapeName(measurement.name)

    val lineTags =
      measurement
        .tags
        .map(t => s"${escapeTag(t.name)}=${escapeTag(t.value)}")
        .emptyAsNone
        .fold("") {
          _.mkString("", ",", " ")
        }

    val lineFields =
      measurement
        .fields
        .map(f => s"${escapeField(f.name)}=${writeFieldValue(f.value)}")
        .emptyAsNone
        .fold("") {
          _.mkString("", ",", " ")
        }

    val lineTime =
      measurement
        .time
        .toEpochMilli
        .toString

    s"${lineMeasurement} ${lineTags}${lineFields}${lineTime}\n"

  extension (measurement: Measurement)
    def toLineProtocol: String = writeToLineProtocol(measurement)

  private def escapeName(string: String): String =
    string
      .replaceAll("\\,", "\\,")
      .trim()

  private def escapeTag(string: String): String =
    string
      .replaceAll("\\s+", "\\ ")
      .replaceAll("=", "\\=")
      .replaceAll("\\,", "\\,")
      .trim()

  private def escapeField(string: String): String =
    string
      .replaceAll("\\s+", "\\ ")
      .replaceAll("\\,", "\\,")
      .trim()

  private def escapeStringValue(string: String): String =
    string.replaceAll("\"", "\\\"")
      .trim()

  extension [A](xs: Vector[A])
    private def emptyAsNone: Option[Vector[A]] = if xs.isEmpty then None else Some(xs)

  private def writeFieldValue(fieldValue: FieldValue): String =
    fieldValue match
      case FieldValue.MString(value)   => s"\"$escapeStringValue(value)\""
      case FieldValue.MFloat(value)    => value.toString
      case FieldValue.MInteger(value)  => value.toString
      case FieldValue.MUInteger(value) => value.toString
      case FieldValue.MBoolean(value)  => value.toString
