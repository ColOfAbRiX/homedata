package com.colofabrix.scala.homedata.tado

import cats.*
import cats.implicits.given
import com.colofabrix.scala.homedata.tado.*
import com.colofabrix.scala.tado4s.api.DayReportResponse
import com.colofabrix.scala.tado4s.api.DayReportResponse.*
import com.colofabrix.scala.tado4s.api.DayReportResponse.ValueType.*
import java.time.*
import java.time.temporal.ChronoUnit
import scala.concurrent.duration.*

object ReportConverter:

  private def TimeResolution =
    5.minutes

  def convert(report: DayReportResponse): Vector[TadoReading] =
    val temperatures       = getInsideTemperatures(report)
    val humidities         = getHumidity(report)
    val weatherTemperature = getWeatherCondition(report.weather)
    val allData            = List(temperatures, humidities, weatherTemperature).combineAll
    allData.toRawMapM.foreach(println)
    Vector.empty

  private def getInsideTemperatures(report: DayReportResponse): DataStore =
    report
      .measuredData
      .insideTemperature
      .dataPoints
      .foldMap {
        case TimeSeriesType.DataPoints(time, Temperature(temperature, _)) =>
          DataStore(time, TadoRunningReading(temperature = Some(temperature)))
      }

  private def getHumidity(report: DayReportResponse): DataStore =
    report
      .measuredData
      .humidity
      .dataPoints
      .foldMap {
        case TimeSeriesType.DataPoints(time, humidity) =>
          DataStore(time, TadoRunningReading(humidity = Some(humidity)))
      }

  private def getWeatherCondition(weather: Weather): DataStore =
    weather
      .condition
      .dataIntervals
      .foldMap {
        case TimeSeriesType.DataIntervals(from, to, WeatherCondition(state, Temperature(temperature, _))) =>
          val reading = TadoRunningReading(outsideTemperature = Some(temperature), outsideState = Some(state))
          DataStore(from, to, reading)
      }

  //  Data Store  //

  private type DataStore =
    TimeSlots[TadoRunningReading]

  private object DataStore:
    def apply(): DataStore =
      TimeSlots[TadoRunningReading](TimeResolution)

    def apply(time: OffsetDateTime, reading: TadoRunningReading): DataStore =
      TimeSlots[TadoRunningReading](TimeResolution, time, reading)

    def apply(from: OffsetDateTime, to: OffsetDateTime, reading: TadoRunningReading): DataStore =
      TimeSlots[TadoRunningReading](TimeResolution, from, to, reading)

  private given Monoid[DataStore] with
    def empty: DataStore =
      DataStore()
    def combine(x: DataStore, y: DataStore): DataStore =
      TimeSlots.given_Semigroup_TimeSlots.combine(x, y)

  //  TadoRunningReading  //

  final private[tado] case class TadoRunningReading(
    atHome: Option[Boolean] = None,
    windowOpen: Option[Boolean] = None,
    temperature: Option[Double] = None,
    humidity: Option[Double] = None,
    outsideTemperature: Option[Double] = None,
    outsideState: Option[String] = None,
    outsideSunny: Option[Boolean] = None,
    setTemperature: Option[Double] = None,
    heatingModulation: Option[Double] = None,
  )

  private[tado] object TadoRunningReading:
    given Show[TadoRunningReading] with
      def show(t: TadoRunningReading): String = t.toString()

    given Monoid[TadoRunningReading] with
      def empty: TadoRunningReading =
        TadoRunningReading()
      def combine(x: TadoRunningReading, y: TadoRunningReading): TadoRunningReading =
        TadoRunningReading(
          atHome = (x.atHome, y.atHome).last,
          windowOpen = (x.windowOpen, y.windowOpen).last,
          temperature = (x.temperature, y.temperature).avg,
          humidity = (x.humidity, y.humidity).avg,
          outsideTemperature = (x.outsideTemperature, y.outsideTemperature).avg,
          outsideState = (x.outsideState, y.outsideState).last,
          outsideSunny = (x.outsideSunny, y.outsideSunny).last,
          setTemperature = (x.setTemperature, y.setTemperature).avg,
          heatingModulation = (x.heatingModulation, y.heatingModulation).avg,
        )

    extension [A](self: (Option[A], Option[A]))
      def last: Option[A] =
        self._2
      def avg(using A: Fractional[A]): Option[A] =
        val list = self.toList.flatMap(_.toList)
        list.reduceOption(A.plus).map(A.div(_, A.fromInt(list.length)))
