package com.colofabrix.scala.timeflux.measurements

import fs2.Pipe

/**
 * Data that can be serialized into a measurement to be written in InfluxDB
 */
trait InfluxSerializable[A]:
  /** Transform the data into a measurement */
  def toMeasurement(a: A): Measurement

  /** Transform a stream of data into a measurement */
  def toStreamMeasurements[F[_]]: Pipe[F, A, Measurement] =
    _.map(toMeasurement)

  extension [A: InfluxSerializable](self: A)
    /** Transform the data into a measurement */
    def toMeasurement: Measurement =
      InfluxSerializable[A].toMeasurement(self)

    /** Transforms the data into an InfluxDB Line Protocol entry */
    def toLineProtocol: String =
      MeasurementWriter.writeToLineProtocol(self.toMeasurement)

object InfluxSerializable:

  def apply[A](using ev: InfluxSerializable[A]): InfluxSerializable[A] =
    ev
