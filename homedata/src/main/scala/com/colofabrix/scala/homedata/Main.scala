package com.colofabrix.scala.homedata

// import com.colofabrix.scala.homedata.octopus.*
import cats.effect.*
import com.colofabrix.scala.homedata.influx.InfluxDbConfig
import com.colofabrix.scala.homedata.influx.InfluxDbConfig.{config => InfluxConf}
import com.colofabrix.scala.homedata.tado.*
import com.colofabrix.scala.timeflux.*
import com.colofabrix.scala.timeflux.api.TimePrecision
import java.time.*
import java.time.temporal.ChronoUnit

object Main extends IOApp.Simple with TimefluxDSL:

  val periodFrom = OffsetDateTime.now.minus(1, ChronoUnit.MONTHS)
  val periodTo   = OffsetDateTime.now

  val run =
    for {
      timefluxClient <- TimefluxClient[IO](InfluxDbConfig.clientConfig)
      _              <- timefluxClient.createBucketIfMissing(InfluxConf.projectBucket)
      tadoPuller     <- TadoPuller()
      tadoReading     = tadoPuller.pullReadings(periodFrom, periodTo)
      // octopus         = octpusReadings(periodFrom) merge tadoMeasurements(periodFrom)
      // allReadings    <- octopus merge tado
      _ <- timefluxClient.writeData(InfluxConf.projectBucket, tadoReading, Some(TimePrecision.Milliseconds))
    } yield ()
