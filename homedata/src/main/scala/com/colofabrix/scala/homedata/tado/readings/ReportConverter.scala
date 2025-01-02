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
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

object ReportConverter:

  implicit private val logger: Logger[IO] =
    Slf4jLogger.getLogger[IO]

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

  private def getInsideTemperatures(insideTemperature: Measure.DataPoints[ValueType.Temperature]): IO[TadoDataStore] =
    IO {
      insideTemperature
        .dataPoints
        .foldMap {
          case TimeSeriesType.DataPoints(time, Temperature(temperature, _)) =>
            TadoDataStore(time, TadoReading.build(temperature = Some(temperature)))
        }
    }

  private def getHumidity(humidity: Measure.DataPoints[ValueType.Percentage]): IO[TadoDataStore] =
    IO {
      humidity
        .dataPoints
        .foldMap {
          case TimeSeriesType.DataPoints(time, humidity) =>
            TadoDataStore(time, TadoReading.build(humidity = Some(humidity)))
        }
    }

  private def getSetTemperature(settings: Measure.DataIntervals[ValueType.HeatingSetting]): IO[TadoDataStore] =
    IO {
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
    IO {
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
    IO {
      sunny
        .dataIntervals
        .foldMap {
          case TimeSeriesType.DataIntervals(from, to, isSunny) =>
            TadoDataStore(from, to, TadoReading.build(outsideSun = Some(isSunny)))
        }
    }

  private def getHeatingModulation(callForHeat: Measure.DataIntervals[ValueType.CallForHeat]): IO[TadoDataStore] =
    IO {
      callForHeat
        .dataIntervals
        .foldMap {
          case TimeSeriesType.DataIntervals(from, to, callForHeat) =>
            TadoDataStore(from, to, TadoReading.build(heatingModulation = Some(callForHeat.dbValue.toDouble)))
        }
    }

  private def getInfoFromStripes(stripes: Measure.DataIntervals[ValueType.Stripes]): IO[TadoDataStore] =
    stripes
      .dataIntervals
      .foldMap {
        case dataInterval @ TimeSeriesType.DataIntervals(from, to, ValueType.Stripes(stripeType, _)) =>
          stripeType.toUpperCase match {
            case "AWAY" =>
              IO {
                val reading = TadoReading.build(atHome = Some(false), windowOpen = Some(false), manualSet = Some(false))
                TadoDataStore(from, to, reading)
              }
            case "HOME" =>
              IO {
                val reading = TadoReading.build(atHome = Some(true), windowOpen = Some(false), manualSet = Some(false))
                TadoDataStore(from, to, reading)
              }
            case "OPEN_WINDOW_DETECTED" =>
              IO {
                val reading = TadoReading.build(atHome = Some(true), windowOpen = Some(true), manualSet = Some(false))
                TadoDataStore(from, to, reading)
              }
            case "OVERLAY_ACTIVE" =>
              IO {
                val reading = TadoReading.build(atHome = Some(true), windowOpen = Some(false), manualSet = Some(true))
                TadoDataStore(from, to, reading)
              }
            case "MEASURING_DEVICE_DISCONNECTED" =>
              IO {
                val reading = TadoReading.build(atHome = Some(true), windowOpen = Some(false), manualSet = Some(true))
                TadoDataStore(from, to, reading)
              }
            case unknown =>
              logger.warn(s" *** Unknown stripe type: '$unknown'") >>
              IO {
                TadoDataStore(from, to, TadoReading.build())
              }
          }
      }
