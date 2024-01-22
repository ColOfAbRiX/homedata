package com.colofabrix.scala.timeflux

import fs2.Pipe
import com.colofabrix.scala.timeflux.model.Measurement

trait InfluxSerializable[A]:
  def toMeasurement(a: A): Measurement

  def toStreamMeasurements[F[_]]: Pipe[F, A, Measurement] =
    _.map(toMeasurement)

  extension [A: InfluxSerializable](self: A)
    def toMeasurement: Measurement =
      InfluxSerializable[A].toMeasurement(self)

    def toLineProtocol: String =
      MeasurementWriter.writeToLineProtocol(self.toMeasurement)

object InfluxSerializable:

  def apply[A](using ev: InfluxSerializable[A]): InfluxSerializable[A] =
    ev
