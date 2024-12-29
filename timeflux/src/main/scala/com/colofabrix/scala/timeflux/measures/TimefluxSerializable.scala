package com.colofabrix.scala.timeflux.measures

import com.colofabrix.scala.timeflux.model.LineProtocolValue
import com.colofabrix.scala.timeflux.api.TimePrecision

/**
 * Data that can be serialized into a measure to be written in InfluxDB
 */
trait TimefluxSerializable[A]:

  /** Transform the data into a measure */
  def toMeasure(a: A): Measure

  /** Transforms the data into an InfluxDB Line Protocol entry */
  def toLineProtocol(a: A): LineProtocolValue =
    MeasureWriter.writeToLineProtocol(toMeasure(a))

  extension [A: TimefluxSerializable](self: A)

    /** Transform the data into a measure */
    def toMeasure: Measure =
      TimefluxSerializable[A].toMeasure(self)

    /** Transforms the data into an InfluxDB Line Protocol entry */
    def toLineProtocol: LineProtocolValue =
      TimefluxSerializable[A].toLineProtocol(self)

object TimefluxSerializable:

  def apply[A](using ev: TimefluxSerializable[A]): TimefluxSerializable[A] =
    ev

  def toApiMeasureStream[F[_], A](apiTimePrecision: TimePrecision)(using ev: TimefluxSerializable[A]): fs2.Pipe[F, A, Measure] =
    _.map(ev.toMeasure)
