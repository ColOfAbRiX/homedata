package com.colofabrix.scala.homedata.tado.readings

import cats.*
import cats.effect.*
import cats.effect.implicits.given
import cats.implicits.given
import com.colofabrix.scala.homedata.tado.*
import com.colofabrix.scala.homedata.tado.store.TadoDataStore
import com.colofabrix.scala.homedata.tado.store.TadoDataStore.given
import com.colofabrix.scala.tado4s.api.DayReportResponse
import com.colofabrix.scala.tado4s.api.DayReportResponse.*
import com.colofabrix.scala.tado4s.api.DayReportResponse.ValueType.*
import java.time.OffsetDateTime

object ReportConverter:

  def convert(room: String, report: DayReportResponse): IO[Vector[TadoReading]] =
    val getters =
      List(
        getInsideTemperatures(report),
        getHumidity(report),
        getSetTemperature(report.settings),
        getWeatherCondition(report.weather.condition),
        getOutsideSun(report.weather.sunny),
        getHeatingModulation(report.callForHeat),
      )

    getters
      .parSequence
      .map { disjointReadings =>
        disjointReadings
          .combineAll
          .toRawMapM
          .toVector
          .map { (time, reading) =>
            reading.copy(room = Some(room), time = Some(time))
          }
      }

  private def getInsideTemperatures(report: DayReportResponse): IO[TadoDataStore] =
    IO.pure {
      report
        .measuredData
        .insideTemperature
        .dataPoints
        .foldMap {
          case TimeSeriesType.DataPoints(time, Temperature(temperature, _)) =>
            TadoDataStore(time, TadoReading(temperature = Some(temperature)))
        }
    }

  private def getHumidity(report: DayReportResponse): IO[TadoDataStore] =
    IO.pure {
      report
        .measuredData
        .humidity
        .dataPoints
        .foldMap {
          case TimeSeriesType.DataPoints(time, humidity) =>
            TadoDataStore(time, TadoReading(humidity = Some(humidity)))
        }
    }

  private def getSetTemperature(settings: Measure.DataIntervals[ValueType.HeatingSetting]): IO[TadoDataStore] =
    IO.pure {
      settings
        .dataIntervals
        .foldMap {
          case TimeSeriesType.DataIntervals(from, to, ValueType.HeatingSetting(_, _, Some(Temperature(temp, _)))) =>
            val reading = TadoReading(setTemperature = Some(temp))
            TadoDataStore(from, to, reading)
          case TimeSeriesType.DataIntervals(_, _, ValueType.HeatingSetting(_, _, None)) =>
            TadoDataStore()
        }
    }

  private def getWeatherCondition(condition: Measure.DataIntervals[ValueType.WeatherCondition]): IO[TadoDataStore] =
    IO.pure {
      condition
        .dataIntervals
        .foldMap {
          case TimeSeriesType.DataIntervals(from, to, WeatherCondition(state, Temperature(temperature, _))) =>
            val reading = TadoReading(outsideTemperature = Some(temperature), outsideState = Some(state.dbValue))
            TadoDataStore(from, to, reading)
        }
    }

  private def getOutsideSun(sunny: Measure.DataIntervals[ValueType.Bool]): IO[TadoDataStore] =
    IO.pure {
      sunny
        .dataIntervals
        .foldMap {
          case TimeSeriesType.DataIntervals(from, to, isSunny) =>
            val reading = TadoReading(outsideSun = Some(isSunny))
            TadoDataStore(from, to, reading)
        }
    }

  private def getHeatingModulation(callForHeat: Measure.DataIntervals[ValueType.CallForHeat]): IO[TadoDataStore] =
    IO.pure {
      callForHeat
        .dataIntervals
        .foldMap {
          case cfh @ TimeSeriesType.DataIntervals(from, to, callForHeat) =>
            val reading = TadoReading(heatingModulation = Some(callForHeat.dbValue.toDouble))
            TadoDataStore(from, to, reading)
        }
    }
