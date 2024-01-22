package com.colofabrix.scala.timeflux.measurements

/**
  * Serializes a Measurement into a Line Protocol value
  */
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

  extension [A](xs: Vector[A])
    private def emptyAsNone: Option[Vector[A]] = if xs.isEmpty then None else Some(xs)

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
    string
      .replaceAll("\"", "\\\"")
      .trim()

  private def writeFieldValue(fieldValue: FieldValue): String =
    fieldValue match
      case FieldValue.StringValue(value)   => s"\"$escapeStringValue(value)\""
      case FieldValue.FloatValue(value)    => value.toString
      case FieldValue.IntegerValue(value)  => value.toString
      case FieldValue.UIntegerValue(value) => value.toString
      case FieldValue.BooleanValue(value)  => value.toString
