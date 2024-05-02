package com.colofabrix.scala.homedata.tado.readings

import cats.*
import cats.effect.*
import cats.effect.implicits.given
import cats.effect.unsafe.implicits.given
import cats.implicits.given
import com.colofabrix.scala.homedata.tado.*
import com.colofabrix.scala.homedata.tado.store.TadoDataStore
import com.colofabrix.scala.homedata.tado.store.TadoDataStore.given
import com.colofabrix.scala.tado4s.api.DayReportResponse
import com.colofabrix.scala.tado4s.api.DayReportResponse.*
import com.colofabrix.scala.tado4s.api.DayReportResponse.ValueType.*
import io.github.arainko.ducktape.*
import java.time.OffsetDateTime
import readings.TadoReading
import readings.TadoRunningReading

object ReportConverter:

  def convert(room: String, report: DayReportResponse): IO[Vector[TadoReading]] =
    List(getInsideTemperatures(report), getHumidity(report), getWeatherCondition(report.weather))
      .parSequence
      .map { disjointReadings =>
        disjointReadings
          .combineAll
          .toRawMapM
          .toVector
          .map { (time, reading) => adaptRunningReading(time, room, reading) }
      }

  private def adaptRunningReading(time: OffsetDateTime, room: String, reading: TadoRunningReading): TadoReading =
    reading
      .into[TadoReading]
      .transform(
        Field.const(_.time, time),
        Field.const(_.room, room),
        Field.computed(_.temperature, _.temperature.getOrElse(0.0)),
        Field.computed(_.humidity, _.humidity.getOrElse(0.0)),
        Field.computed(_.outsideTemperature, _.outsideTemperature.getOrElse(0.0)),
        Field.computed(_.setTemperature, _.setTemperature.getOrElse(0.0)),
      )

  private def getInsideTemperatures(report: DayReportResponse): IO[TadoDataStore] =
    IO {
      report
        .measuredData
        .insideTemperature
        .dataPoints
        .foldMap {
          case TimeSeriesType.DataPoints(time, Temperature(temperature, _)) =>
            TadoDataStore(time, TadoRunningReading(temperature = Some(temperature)))
        }
    }

  private def getHumidity(report: DayReportResponse): IO[TadoDataStore] =
    IO {
      report
        .measuredData
        .humidity
        .dataPoints
        .foldMap {
          case TimeSeriesType.DataPoints(time, humidity) =>
            TadoDataStore(time, TadoRunningReading(humidity = Some(humidity)))
        }
    }

  private def getWeatherCondition(weather: Weather): IO[TadoDataStore] =
    IO {
      weather
        .condition
        .dataIntervals
        .foldMap {
          case TimeSeriesType.DataIntervals(from, to, WeatherCondition(state, Temperature(temperature, _))) =>
            val reading = TadoRunningReading(outsideTemperature = Some(temperature), outsideState = Some(state))
            TadoDataStore(from, to, reading)
        }
    }
