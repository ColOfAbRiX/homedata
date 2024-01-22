package com.colofabrix.scala.homedata

// import java.time.temporal.ChronoUnit
import cats.implicits.given
import cats.effect.*
import com.colofabrix.scala.homedata.octopus.*
import com.colofabrix.scala.homedata.octopus.Reading.given
import com.colofabrix.scala.timeflux.*
import java.time.*

object Main extends IOApp.Simple:
  val periodTo   = Instant.now()
  val periodFrom = Instant.parse("2023-07-07T00:00:00.00Z")

  val run =
    Timeflux
      .client[IO](InfluxDB.config.serverUri, InfluxDB.config.organizationId, InfluxDB.config.authToken)
      .use { timefluxClient =>
        println(OctopusConfig.config)
        for
          _                  <- timefluxClient.createBucketIfMissing(InfluxDB.config.octopusBucket)
          electricityReadings = Tools.pullPaged(OctopusElectricity.pullPage(periodFrom))
          gasReadings         = Tools.pullPaged(OctopusGas.pullPage(periodFrom))
          allReadings         = (electricityReadings merge gasReadings).widen[Reading]
          result             <- timefluxClient.write(InfluxDB.config.octopusBucket, allReadings)
        yield result
      }
