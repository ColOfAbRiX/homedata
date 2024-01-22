package com.colofabrix.scala.homedata

import cats.implicits.*
import java.time.*
import java.time.temporal.ChronoUnit
import reflux.*
import org.http4s.Uri
import reflux.api.*
// import fs2.Stream

final case class ElectricityReading(time: Instant, value: Double)

@main def main: Unit =
  val periodTo   = Instant.now()
  val periodFrom = periodTo.minus(3, ChronoUnit.DAYS)

  Reflux
    .clientIO(
      Uri.unsafeFromString("http://127.0.0.1:8086"),
      OrganizationId("dd87acad77d7e016"),
      InfluxAuthToken("VRB6CbkkF4CcAG8dKHu56D_t8kA4IumEMI5-QazdKz0ry3vArEe3tOCSw3YvgTnHinIEicUXpEC-5rI3Zu1rjQ=="),
    )
    .createDatabase("octopus_electricity")
    .unsafeRunSync()(cats.effect.unsafe.IORuntime.global)
    .unit

  // Stream
  //   .unfoldChunk(1) { page =>
  //     val pageSize = 100
  //     val readings = Electricity.readConsumption(Some(periodFrom), Some(periodTo), page, pageSize)
  //     if (readings.size < pageSize) None else Some((readings, page + 1))
  //   }
  //   .map { reading => }
  //   .toList
  //   .unit

extension [A](a: A) def unit: Unit = ()
