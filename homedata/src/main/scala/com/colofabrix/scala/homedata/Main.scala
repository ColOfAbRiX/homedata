package com.colofabrix.scala.homedata

import cats.effect.*
import com.colofabrix.scala.cuttlefish.*
import com.colofabrix.scala.cuttlefish.api.*
import com.colofabrix.scala.homedata.influx.InfluxDbConfig
import com.colofabrix.scala.homedata.influx.InfluxDbConfig.{ config => InfluxConf }
import com.colofabrix.scala.homedata.octopus.Octopus
import com.colofabrix.scala.homedata.octopus.OctopusConfig.{ config => OctoConf }
import com.colofabrix.scala.homedata.tado.*
import com.colofabrix.scala.timeflux.*
import com.colofabrix.scala.timeflux.measures.TimefluxSerializable
import java.time.*

object Main extends IOApp.Simple with TimefluxDSL with CuttlefishDSL:

  val from = OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
  val to   = OffsetDateTime.now

  val run =
    for {
      octoClient <- CuttlefishClient[IO]()
      _          <- octoClient.login(OctoConf.apiKey)
      gas        <- octoClient.meterConsumption(OctopusProduct.Gas, OctoConf.gasMprn, OctoConf.gasSerial, Some(from), Some(to))
      electricity <- octoClient.meterConsumption(
                       OctopusProduct.Electricity,
                       OctoConf.electricityMpan,
                       OctoConf.electricitySerial,
                       Some(from),
                       Some(to),
                     )
      _ <- IO.println(gas)
      _ <- IO.println(electricity)
      _ <- IO.sleep(scala.concurrent.duration.FiniteDuration(15, "seconds"))
      // Tado
      tadoPuller  <- TadoPuller()
      tadoReading  = tadoPuller.pullReadings(from, to)
      tadoMeasures = tadoReading.through(TimefluxSerializable.toApiMeasureStream)
      // Octopus
      octopusReadings = Octopus.pullReadings(from, to)
      octopusMeasures = octopusReadings.through(TimefluxSerializable.toApiMeasureStream)
      // Timeflux
      timefluxClient <- TimefluxClient[IO](InfluxDbConfig.clientConfig)
      _              <- timefluxClient.createBucketIfMissing(InfluxConf.projectBucket)
      allMeasures     = tadoMeasures merge octopusMeasures
      _ <- timefluxClient.writeMeasures(
             InfluxConf.projectBucket,
             allMeasures,
             batchWrites = Some(InfluxConf.batchWrites),
           )
    } yield ()
