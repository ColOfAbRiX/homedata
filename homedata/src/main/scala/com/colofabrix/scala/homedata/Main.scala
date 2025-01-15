package com.colofabrix.scala.homedata

import cats.effect.*
import com.colofabrix.scala.homedata.influx.*
import com.colofabrix.scala.homedata.octopus.*
import com.colofabrix.scala.homedata.tado.*
import com.colofabrix.scala.timeflux.*
import com.colofabrix.scala.timeflux.measures.TimefluxSerializable.toApiMeasureStream
import java.time.*
import java.time.temporal.ChronoUnit

object Main extends IOApp.Simple:

  val (from, to) =
    TimeSpanPicker()
      .selectFrom()
      .setDate(2024, 1, 1)
      .otherMinus(months = 1)
      .roundBoth(ChronoUnit.DAYS)
      .pick()

  val run =
    for
      octoPuller         <- OctopusPuller()
      gasMeasures         = octoPuller.pullGasReadings(from, to).through(toApiMeasureStream)
      electricityMeasures = octoPuller.pullElectricityReadings(from, to).through(toApiMeasureStream)
      tadoPuller         <- TadoPuller()
      tadoMeasures        = tadoPuller.pullReadings(from, to).through(toApiMeasureStream)
      writer             <- InfluxWriter()
      _                  <- writer.write(tadoMeasures, gasMeasures, electricityMeasures)
    yield ()
