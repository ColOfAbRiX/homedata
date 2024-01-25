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
  stripes: MeasureType.DataIntervalsMeasure[StripeValue],
  settings: MeasureType.DataIntervalsMeasure[SettingClass],
  callForHeat: MeasureType.DataIntervalsMeasure[String],
  hotWaterProduction: MeasureType.DataIntervalsMeasure[Boolean],
  weather: Weather,
) derives Decoder

object DayReportResponse:

  transparent trait MeasureType

  object MeasureType:

    final case class DataIntervalsMeasure[A](
      timeSeriesType: String, // Always "dataIntervals"
      valueType: String,
      dataIntervals: Vector[TimeSeriesType.DataIntervals[A]],
    ) extends MeasureType derives Decoder

    final case class HumidityMeasure(
      timeSeriesType: String, // Always "dataPoints"
      valueType: String,
      min: Double,
      max: Double,
      percentageUnit: String,
      dataPoints: Vector[TimeSeriesType.DataPoint[Double]],
    ) extends MeasureType derives Decoder

    final case class TemperatureMeasure(
      timeSeriesType: String, // Always "dataPoints"
      valueType: String,
      min: Temperature,
      max: Temperature,
      dataPoints: Vector[TimeSeriesType.DataPoint[Temperature]],
    ) extends MeasureType derives Decoder

    final case class SlotsMeasure(
      timeSeriesType: String, // Always "slots"
      valueType: String,      // Always "slot"
      slots: Map[String, TimeSeriesType.Slot],
    ) extends MeasureType derives Decoder

  transparent trait TimeSeriesType

  object TimeSeriesType:

    final case class DataIntervals[A](
      from: String,
      to: String,
      value: A,
    ) extends TimeSeriesType derives Decoder

    final case class DataPoint[A](
      timestamp: Instant,
      value: A,
    ) extends TimeSeriesType derives Decoder

    final case class Slot(
      state: String,
      temperature: Temperature,
    ) extends TimeSeriesType derives Decoder

  final case class Interval(
    from: String,
    to: String,
  ) derives Decoder

  final case class MeasuredData(
    measuringDeviceConnected: MeasureType.DataIntervalsMeasure[Boolean],
    insideTemperature: MeasureType.TemperatureMeasure,
    humidity: MeasureType.HumidityMeasure,
  ) derives Decoder

  final case class Temperature(
    celsius: Double,
    fahrenheit: Double,
  ) derives Decoder

  final case class SettingClass(
    `type`: String,
    power: String,
    temperature: Temperature,
  ) derives Decoder

  final case class StripeValue(
    stripeType: String,
    setting: SettingClass,
  ) derives Decoder

  final case class Weather(
    condition: MeasureType.DataIntervalsMeasure[TimeSeriesType.Slot],
    sunny: MeasureType.DataIntervalsMeasure[Boolean],
    slots: MeasureType.SlotsMeasure,
  ) derives Decoder
