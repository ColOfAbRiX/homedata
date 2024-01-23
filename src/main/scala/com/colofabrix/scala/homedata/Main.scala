package com.colofabrix.scala.homedata

import cats.effect.*
import cats.implicits.given
import com.colofabrix.scala.homedata.octopus.*
import com.colofabrix.scala.homedata.octopus.OctopusReading.given
import com.colofabrix.scala.timeflux.*
import java.time.*

object Main extends IOApp.Simple:
  val periodFrom = Instant.parse("2023-07-07T00:00:00.00Z")

  val run =
    Timeflux
      .client[IO](InfluxDB.config.serverUri, InfluxDB.config.organizationId, InfluxDB.config.authToken)
      .use { timefluxClient =>
        for
          _              <- timefluxClient.createBucketIfMissing(InfluxDB.config.octopusBucket)
          octopusReadings = octpusReadings(periodFrom)
          result         <- timefluxClient.write(InfluxDB.config.octopusBucket, octopusReadings)
        yield result
      }

  private def octpusReadings(from: Instant): fs2.Stream[IO, OctopusReading] =
    val electricityReadings = Tools.pullPaged(OctopusElectricity.pullPage(from))
    val gasReadings         = Tools.pullPaged(OctopusGas.pullPage(from))
    (electricityReadings merge gasReadings).widen[OctopusReading]
