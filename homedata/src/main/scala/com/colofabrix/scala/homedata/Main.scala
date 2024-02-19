package com.colofabrix.scala.homedata

import cats.effect.*
import com.colofabrix.scala.homedata.octopus.*
import com.colofabrix.scala.homedata.tado.*
import com.colofabrix.scala.timeflux.*
import com.colofabrix.scala.timeflux.measurements.Measurement
import java.time.*
import java.time.temporal.ChronoUnit

object Main extends IOApp.Simple:
  // val periodFrom = OffsetDateTime.parse("2024-01-25T00:00:00.00Z")
  val periodFrom = OffsetDateTime.now().minus(1, ChronoUnit.DAYS)

  val run =
    Timeflux
      .client[IO](InfluxDB.config.serverUri, InfluxDB.config.organizationId, InfluxDB.config.authToken)
      .use { timefluxClient =>
        for
          _ <- timefluxClient.createBucketIfMissing(InfluxDB.config.octopusBucket)
          // readings  = octpusReadings(periodFrom) merge tadoReadings(periodFrom)
          readings = tadoReadings(periodFrom)
          result  <- timefluxClient.write(InfluxDB.config.octopusBucket, readings)
        // readings <- tadoReadings(periodFrom).compile.toVector
        // _         = readings.foreach(println)
        yield ()
      }

  private def octpusReadings(from: OffsetDateTime): fs2.Stream[IO, Measurement] =
    val electricityReadings =
      Tools
        .pullPaged(OctopusElectricity.pullPage(from))
        .map(_.toMeasurement)

    val gasReadings =
      Tools
        .pullPaged(OctopusGas.pullPage(from))
        .map(_.toMeasurement)

    (electricityReadings merge gasReadings)

  private def tadoReadings(from: OffsetDateTime): fs2.Stream[IO, Measurement] =
    fs2.Stream
      .eval(Tado())
      .flatMap { tadoPuller =>
        Tools
          .pullRange(from.toLocalDate(), Tado.getNextDate(LocalDate.now(), _)) { day =>
            tadoPuller.pullDate(day)
          }
      }
      .map(_.toMeasurement)
