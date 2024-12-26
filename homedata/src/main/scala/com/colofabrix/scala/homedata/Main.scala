package com.colofabrix.scala.homedata

// import com.colofabrix.scala.homedata.octopus.*
import cats.effect.*
import com.colofabrix.scala.homedata.influx.InfluxDbConfig
import com.colofabrix.scala.homedata.influx.InfluxDbConfig.{ config => InfluxConf }
import com.colofabrix.scala.homedata.tado.*
import com.colofabrix.scala.timeflux.*
import com.colofabrix.scala.timeflux.api.TimePrecision
import com.colofabrix.scala.timeflux.measures.Measure
import fs2.concurrent.Channel
import java.time.*
// import java.time.temporal.ChronoUnit

object Main extends IOApp.Simple with TimefluxDSL:

  // val periodFrom = OffsetDateTime.now.minus(1, ChronoUnit.WEEKS)
  // val periodTo   = OffsetDateTime.now
  //val periodTo = OffsetDateTime.of(2024, 12, 1, 1, 0, 0, 0, ZoneOffset.UTC)  // Away, Default, Manual, Heat
  // val periodTo = OffsetDateTime.of(2024, 12, 14, 1, 0, 0, 0, ZoneOffset.UTC)  // Default, Away, Heat, Window
  // val periodTo = OffsetDateTime.of(2024, 12, 15, 1, 0, 0, 0, ZoneOffset.UTC)  // Default, Off
  // val periodFrom =  periodTo.minus(1, ChronoUnit.DAYS)
  val periodFrom = OffsetDateTime.of(2024, 12, 13, 1, 0, 0, 0, ZoneOffset.UTC)
  val periodTo = OffsetDateTime.of(2024, 12, 15, 1, 0, 0, 0, ZoneOffset.UTC)

  val run =
    for {
      timefluxClient <- TimefluxClient[IO](InfluxDbConfig.clientConfig)
      _              <- timefluxClient.createBucketIfMissing(InfluxConf.projectBucket)
      channel        <- Channel.unbounded[IO, Measure]
      tadoPuller     <- TadoPuller()
      tadoReading     = tadoPuller.pullReadings(periodFrom, periodTo)
      // octopus         = octpusReadings(periodFrom) merge tadoMeasurements(periodFrom)
      // allReadings    <- octopus merge tado
      _ <- timefluxClient.writeData(InfluxConf.projectBucket, tadoReading, Some(TimePrecision.Seconds))
    } yield ()
