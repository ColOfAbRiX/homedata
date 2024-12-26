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
        getHeatingModulation(report.callForHeat),
        getHumidity(report.measuredData.humidity),
        getInfoFromStripes(report.stripes),
        getInsideTemperatures(report.measuredData.insideTemperature),
        getOutsideSun(report.weather.sunny),
        getSetTemperature(report.settings),
        getWeatherCondition(report.weather.condition),
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
      .flatTap { reading =>
        reading.traverse(r => IO.println(s"${r.time} - atHome=${r.atHome}")) >>
        IO.raiseError(new RuntimeException("dd"))
      }

  private def getInsideTemperatures(insideTemperature: Measure.DataPoints[ValueType.Temperature]): IO[TadoDataStore] =
    IO.pure {
      insideTemperature
        .dataPoints
        .foldMap {
          case TimeSeriesType.DataPoints(time, Temperature(temperature, _)) =>
            TadoDataStore(time, TadoReading.build(temperature = Some(temperature)))
        }
    }

  private def getHumidity(humidity: Measure.DataPoints[ValueType.Percentage]): IO[TadoDataStore] =
    IO.pure {
      humidity
        .dataPoints
        .foldMap {
          case TimeSeriesType.DataPoints(time, humidity) =>
            TadoDataStore(time, TadoReading.build(humidity = Some(humidity)))
        }
    }

  private def getSetTemperature(settings: Measure.DataIntervals[ValueType.HeatingSetting]): IO[TadoDataStore] =
    IO.pure {
      settings
        .dataIntervals
        .foldMap {
          case TimeSeriesType.DataIntervals(from, to, ValueType.HeatingSetting(_, _, Some(Temperature(temp, _)))) =>
            TadoDataStore(from, to, TadoReading.build(setTemperature = Some(temp)))
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
            val reading =
              TadoReading.build(
                outsideTemperature = Some(temperature),
                outsideState = Some(state.dbValue),
              )

            TadoDataStore(from, to, reading)
        }
    }

  private def getOutsideSun(sunny: Measure.DataIntervals[ValueType.Bool]): IO[TadoDataStore] =
    IO.pure {
      sunny
        .dataIntervals
        .foldMap {
          case TimeSeriesType.DataIntervals(from, to, isSunny) =>
            TadoDataStore(from, to, TadoReading.build(outsideSun = Some(isSunny)))
        }
    }

  private def getHeatingModulation(callForHeat: Measure.DataIntervals[ValueType.CallForHeat]): IO[TadoDataStore] =
    IO.pure {
      callForHeat
        .dataIntervals
        .foldMap {
          case TimeSeriesType.DataIntervals(from, to, callForHeat) =>
            TadoDataStore(from, to, TadoReading.build(heatingModulation = Some(callForHeat.dbValue.toDouble)))
        }
    }

  private def getInfoFromStripes(stripes: Measure.DataIntervals[ValueType.Stripes]): IO[TadoDataStore] =
    IO.pure {
      stripes
        .dataIntervals
        .foldMap {
          case TimeSeriesType.DataIntervals(from, to, ValueType.Stripes(stripeType, _)) =>
            val reading =
              stripeType.toUpperCase match {
                case "AWAY" =>
                  TadoReading.build(atHome = Some(false), windowOpen = Some(false), manualSet = Some(false))
                case "HOME" =>
                  TadoReading.build(atHome = Some(true), windowOpen = Some(false), manualSet = Some(false))
                case "OPEN_WINDOW_DETECTED" =>
                  TadoReading.build(atHome = Some(true), windowOpen = Some(true), manualSet = Some(false))
                case "OVERLAY_ACTIVE" =>
                  TadoReading.build(atHome = Some(true), windowOpen = Some(false), manualSet = Some(true))
                case unknown =>
                  println(s" *** Unknown stripe type: '$unknown'")
                  TadoReading.build()
              }

            println(s"TadoDataStore($from, $to, $reading)")
            TadoDataStore(from, to, reading)
        }
    }
