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
  stripes: Measure.DataIntervals[ValueType.Stripes],
  settings: Measure.DataIntervals[ValueType.HeatingSetting],
  callForHeat: Measure.DataIntervals[ValueType.CallForHeat],
  hotWaterProduction: Measure.DataIntervals[ValueType.Bool],
  weather: Weather,
) derives Decoder

object DayReportResponse:

  transparent trait Measure

  object Measure:

    final case class DataPoints[A](
      timeSeriesType: String,
      valueType: String,
      min: Option[A],
      max: Option[A],
      percentageUnit: Option[String],
      dataPoints: Vector[TimeSeriesType.DataPoints[A]]
    ) extends Measure derives Decoder

    final case class DataIntervals[A](
      timeSeriesType: String,
      valueType: String,
      dataIntervals: Vector[TimeSeriesType.DataIntervals[A]],
    ) extends Measure derives Decoder

    final case class Slots[A](
      timeSeriesType: String,
      valueType: String,
      slots: Map[String, A],
    ) extends Measure derives Decoder

  transparent trait TimeSeriesType

  object TimeSeriesType:

    final case class DataIntervals[A](
      from: OffsetDateTime,
      to: OffsetDateTime,
      value: A,
    ) extends TimeSeriesType derives Decoder

    final case class DataPoints[A](
      timestamp: OffsetDateTime,
      value: A,
    ) extends TimeSeriesType derives Decoder

  transparent trait ValueType

  object ValueType:

    type CallForHeat = String

    type Percentage = Double

    type Bool = Boolean

    final case class HeatingSetting(
      `type`: String,
      power: String,
      temperature: Option[Temperature],
    ) extends ValueType derives Decoder

    final case class Stripes(
      stripeType: String,
      setting: Option[HeatingSetting],
    ) extends ValueType derives Decoder

    final case class Temperature(
      celsius: Double,
      fahrenheit: Double,
    ) extends ValueType derives Decoder

    final case class WeatherCondition(
      state: String,
      temperature: Temperature,
    ) extends ValueType derives Decoder

  final case class Interval(
    from: OffsetDateTime,
    to: OffsetDateTime,
  ) derives Decoder

  final case class MeasuredData(
    measuringDeviceConnected: Measure.DataIntervals[ValueType.Bool],
    insideTemperature: Measure.DataPoints[ValueType.Temperature],
    humidity: Measure.DataPoints[ValueType.Percentage],
  ) derives Decoder

  final case class Weather(
    condition: Measure.DataIntervals[ValueType.WeatherCondition],
    sunny: Measure.DataIntervals[ValueType.Bool],
    slots: Measure.Slots[ValueType.WeatherCondition],
  ) derives Decoder
