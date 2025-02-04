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
      .otherMinus(months = 1)
      .roundBoth(ChronoUnit.DAYS)
      .pick()

  import cats.implicits.given
  import com.colofabrix.scala.timeflux.api.QueryRequest
  val query =
    """from(bucket: "home_data")
      ||>  range(start: -7d)
      ||>  filter(fn: (r) => r["_measurement"] == "gas")
      ||>  filter(fn: (r) => r["_field"] == "consumption")
      ||>  aggregateWindow(every: 1h, fn: mean, createEmpty: false)
      ||>  yield(name: "mean")""".stripMargin

  val run =
    for
      timefluxClient <- TimefluxClient[IO](InfluxConfig.clientConfig)
      points         <- timefluxClient.query(QueryRequest(query, None))
      listPoints     <- points.compile.toList
      _              <- listPoints.traverse(IO.println)
      _              <- IO.raiseError(new RuntimeException("IT'S ALL GOOD, IT WORKED"))
      // Real working Tado/Octopus
      octoPuller         <- OctopusPuller()
      gasMeasures         = octoPuller.pullGasReadings(from, to).through(toApiMeasureStream)
      electricityMeasures = octoPuller.pullElectricityReadings(from, to).through(toApiMeasureStream)
      tadoPuller         <- TadoPuller()
      tadoMeasures        = tadoPuller.pullReadings(from, to).through(toApiMeasureStream)
      writer             <- InfluxWriter()
      _                  <- writer.write(tadoMeasures, gasMeasures, electricityMeasures)
    yield ()
