package com.colofabrix.scala.homedata

import sttp.client4.*
import java.time.*
import java.time.temporal.ChronoUnit
import fs2.Stream

final case class ElectricityReading(time: Instant, value: Double)

@main def homeData: Unit =
  val periodTo   = Instant.now()
  val periodFrom = periodTo.minus(3, ChronoUnit.DAYS)

  Stream
    .unfoldChunk(1) { page =>
      val pageSize = 10
      val readings = Electricity.readConsumption(Some(periodFrom), Some(periodTo), page, pageSize)
      if (readings.size < pageSize) None else Some((readings, page + 1))
    }
    .map { value =>
      println(value)
    }
    .toList
    .unit

extension [A](a: A) def unit: Unit = ()
