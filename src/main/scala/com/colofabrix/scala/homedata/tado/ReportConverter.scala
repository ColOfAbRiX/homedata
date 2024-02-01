package com.colofabrix.scala.homedata.tado

import cats.Align
import cats.data.Ior
import cats.Functor
import cats.implicits.given
import cats.kernel.Monoid
import com.colofabrix.scala.homedata.tado.TimeSlots
import com.colofabrix.scala.tado4s.api.DayReportResponse
import com.colofabrix.scala.tado4s.api.DayReportResponse.*
import java.time.*
import java.time.temporal.ChronoUnit
import scala.concurrent.duration.*

object ReportConverter:

  private def TimeResolution = 5.minutes

  private given [A]: Monoid[TimeSlots[A]] =
    new Monoid[TimeSlots[A]] {
      override def empty: TimeSlots[A] =
        TimeSlots(TimeResolution)
      override def combine(x: TimeSlots[A], y: TimeSlots[A]): TimeSlots[A] =
        x ++ y
    }

  def convert(report: DayReportResponse): Vector[TadoReading] =
    ???

  private def getInsideTemperatures(report: DayReportResponse): Map[OffsetDateTime, Double] =
    report
      .measuredData
      .insideTemperature
      .dataPoints
      .map {
        case TimeSeriesType.DataPoints(time, ValueType.Temperature(temperature, _)) =>
          (time, temperature)
      }
      .toMap

  private def getHumidity(report: DayReportResponse): Map[OffsetDateTime, Double] =
    report
      .measuredData
      .humidity
      .dataPoints
      .map {
        case TimeSeriesType.DataPoints(time, humidity) => (time, humidity)
      }
      .toMap

  // private def getWeatherTemperatures(day: LocalDate, weather: Weather): Map[OffsetDateTime, Double] =
  //   val dayStart = day.atTime(0, 0, 0)
  //   val dayEnd   = day.atTime(23, 59, 59)

  //   val tmp =
  //     weather
  //       .condition
  //       .dataIntervals
  //       .foldMap {
  //         case DataIntervals(from, to, WeatherCondition(state, ValueType.Temperature(temperature, _))) =>
  //           TimeSlots(TimeResolution, from, to, temperature)
  //       }
  //       .toMap(dayStart, dayEnd)

  //   ???
