package com.colofabrix.scala.tado4s.api

import com.colofabrix.scala.tado4s.api.DayReportResponse.*
import io.circe.Decoder
import java.time.*

/**
 * DayReportResponse
 */
final case class DayReportResponse(
  zoneType: String,
  interval: Interval,
  hoursInDay: Int,
  measuredData: MeasuredData,
  stripes: TimeSeriesType.DataIntervals[ValueType.Stripes[ValueType.Temperature]],
  settings: TimeSeriesType.DataIntervals[ValueType.HeatingSetting],
  callForHeat: TimeSeriesType.DataIntervals[ValueType.CallForHeat],
  hotWaterProduction: TimeSeriesType.DataIntervals[ValueType.CallForHeat],
  weather: Weather,
) derives Decoder

object DayReportResponse:

  transparent trait TimeSeriesType

  object TimeSeriesType:

    type Slots[A <: ValueType] = Map[String, A] with TimeSeriesType

    final case class DataIntervals[A <: ValueType](
      from: String,
      to: String,
      value: A,
    ) extends TimeSeriesType derives Decoder

    final case class DataPoints[A <: ValueType](
      timestamp: Instant,
      value: A,
    ) extends TimeSeriesType derives Decoder

  transparent trait ValueType

  object ValueType:

    type CallForHeat = String with ValueType

    type Percentage = Double with ValueType

    type Bool = Boolean with ValueType

    final case class HeatingSetting(
      `type`: String,
      power: String,
      temperature: Option[Temperature],
    ) extends ValueType derives Decoder

    final case class Stripes[A](
      stripeType: String,
      setting: A,
    ) extends ValueType derives Decoder

    final case class Temperature(
      celsius: Double,
      fahrenheit: Double,
    ) extends ValueType derives Decoder

    final case class WeatherCondition(
      state: String,
      temperature: Temperature,
    ) extends ValueType derives Decoder

  final case class Measure[A <: ValueType](
    timeSeriesType: String,
    valueType: String,
    min: Option[A],
    max: Option[A],
    percentageUnit: Option[String],
    dataPoints: Option[TimeSeriesType.DataPoints[A]],
    dataIntervals: Option[TimeSeriesType.DataIntervals[A]],
    // slots: Option[TimeSeriesType.Slots[A]],
  ) derives Decoder

  final case class Interval(
    from: String,
    to: String,
  ) derives Decoder

  final case class MeasuredData(
    // measuringDeviceConnected: TimeSeriesType.DataIntervals[ValueType.Bool],
    insideTemperature: TimeSeriesType.DataPoints[ValueType.Temperature],
    // humidity: TimeSeriesType.DataPoints[ValueType.Percentage],
  ) derives Decoder

  final case class Weather(
    condition: TimeSeriesType.DataIntervals[ValueType.WeatherCondition],
    // sunny: TimeSeriesType.DataIntervals[ValueType.Bool],
    // slots: TimeSeriesType.Slots[ValueType.WeatherCondition],
  ) derives Decoder
