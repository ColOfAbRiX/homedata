package com.colofabrix.scala.homedata.tado.readings

import cats.*
import cats.effect.*
import cats.effect.implicits.given
import cats.implicits.given
import com.colofabrix.scala.homedata.tado.*
import com.colofabrix.scala.homedata.tado.readings.TadoDataStore.given
import com.colofabrix.scala.tado4s.api.DayReportResponse
import com.colofabrix.scala.tado4s.api.DayReportResponse.*
import java.time.OffsetDateTime
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

object ReportConverter {

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
            TadoReading(
              room = Some(room),
              time = Some(time),
              temperature = reading.temperature,
              humidity = reading.humidity,
              setTemperature = reading.setTemperature,
              outsideTemperature = reading.outsideTemperature,
              outsideState = reading.outsideState,
              outsideSun = reading.outsideSun,
              heatingModulation = reading.heatingModulation,
              atHome = reading.atHome,
              windowOpen = reading.windowOpen,
              manualSet = reading.manualSet,
              isOff = reading.isOff,
            )
          }
      }

  private def getInsideTemperatures(insideTemperature: PointsSeries[Temperature]): IO[TadoDataStore] =
    IO {
      insideTemperature
        .dataPoints
        .foldMap {
          case DataPoint(time, Temperature(temperature, _)) =>
            TadoDataStore(time, TadoReading.build(temperature = Some(temperature)))
        }
    }

  private def getHumidity(humidity: PointsSeries[Double]): IO[TadoDataStore] =
    IO {
      humidity
        .dataPoints
        .foldMap {
          case DataPoint(time, humidity) =>
            TadoDataStore(time, TadoReading.build(humidity = Some(humidity)))
        }
    }

  private def getSetTemperature(settings: IntervalSeries[HeatingSetting]): IO[TadoDataStore] =
    IO {
      settings
        .dataIntervals
        .foldMap {
          case DataInterval(from, to, Some(HeatingSetting(_, _, Some(Temperature(temp, _))))) =>
            TadoDataStore(
              from = from,
              to = to,
              reading = TadoReading.build(setTemperature = Some(temp), isOff = Some(false)),
            )
          case DataInterval(from, to, Some(HeatingSetting(_, _, None))) =>
            TadoDataStore(
              from = from,
              to = to,
              reading = TadoReading.build(isOff = Some(true)),
            )
          case DataInterval(_, _, None) =>
            TadoDataStore()
        }
    }

  private def getWeatherCondition(condition: IntervalSeries[WeatherCondition]): IO[TadoDataStore] =
    IO {
      condition
        .dataIntervals
        .foldMap {
          case DataInterval(from, to, Some(WeatherCondition(state, Temperature(temperature, _)))) =>
            val reading =
              TadoReading.build(
                outsideTemperature = Some(temperature),
                outsideState = Some(state.dbValue),
              )

            TadoDataStore(from, to, reading)
          case DataInterval(_, _, None) =>
            TadoDataStore()
        }
    }

  private def getOutsideSun(sunny: IntervalSeries[Boolean]): IO[TadoDataStore] =
    IO {
      sunny
        .dataIntervals
        .foldMap {
          case DataInterval(from, to, Some(isSunny)) =>
            TadoDataStore(from, to, TadoReading.build(outsideSun = Some(isSunny)))
          case DataInterval(_, _, None) =>
            TadoDataStore()
        }
    }

  private def getHeatingModulation(callForHeat: IntervalSeries[CallForHeat]): IO[TadoDataStore] =
    IO {
      callForHeat
        .dataIntervals
        .foldMap {
          case DataInterval(from, to, Some(callForHeat)) =>
            TadoDataStore(from, to, TadoReading.build(heatingModulation = Some(callForHeat.dbValue.toDouble)))
          case DataInterval(_, _, None) =>
            TadoDataStore()
        }
    }

  private def getInfoFromStripes(stripes: IntervalSeries[Stripes]): IO[TadoDataStore] =
    stripes
      .dataIntervals
      .foldMap {
        case DataInterval(from, to, Some(Stripes(stripeType, _))) =>
          IO
            .pure(stripeType.entryName)
            .flatMap {
              case "AWAY" =>
                IO(TadoReading.build(atHome = Some(false), windowOpen = Some(false), manualSet = Some(false)))
              case "HOME" =>
                IO(TadoReading.build(atHome = Some(true), windowOpen = Some(false), manualSet = Some(false)))
              case "OPEN_WINDOW_DETECTED" =>
                IO(TadoReading.build(atHome = Some(true), windowOpen = Some(true), manualSet = Some(false)))
              case "OVERLAY_ACTIVE" =>
                IO(TadoReading.build(atHome = Some(true), windowOpen = Some(false), manualSet = Some(true)))
              case "MEASURING_DEVICE_DISCONNECTED" =>
                IO(TadoReading.build(atHome = Some(true), windowOpen = Some(false), manualSet = Some(true)))
              case unknown =>
                logger.warn(s" *** Unknown stripe type: '$unknown'") >>
                IO(TadoReading.build())
            }
            .map {
              TadoDataStore(from, to, _)
            }
        case DataInterval(_, _, None) =>
          IO.pure(TadoDataStore())
      }

}
