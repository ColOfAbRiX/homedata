package com.colofabrix.scala.tado4s.api

import io.circe.Decoder
import java.time.*

/**
 * DayReportResponse
 */
final case class DayReportResponse(
  zoneType: String,
  interval: DayReportResponse.Interval,
  hoursInDay: Int,
  measuredData: DayReportResponse.MeasuredData,
  stripes: DayReportResponse.Stripes,
  settings: DayReportResponse.ReportSettings,
  callForHeat: DayReportResponse.CallForHeat,
  hotWaterProduction: DayReportResponse.HotWaterProduction,
  weather: DayReportResponse.Weather,
) derives Decoder

object DayReportResponse:

  final case class CallForHeat(
    timeSeriesType: String,
    valueType: String,
    dataIntervals: Vector[CallForHeatDataInterval],
  ) derives Decoder

  final case class CallForHeatDataInterval(
    from: String,
    to: String,
    value: String,
  ) derives Decoder

  final case class HotWaterProduction(
    timeSeriesType: String,
    valueType: String,
    dataIntervals: Vector[HotWaterProductionDataInterval],
  ) derives Decoder

  final case class HotWaterProductionDataInterval(
    from: String,
    to: String,
    value: Boolean,
  ) derives Decoder

  final case class Interval(
    from: String,
    to: String,
  ) derives Decoder

  final case class MeasuredData(
    measuringDeviceConnected: HotWaterProduction,
    insideTemperature: InsideTemperature,
    humidity: Humidity,
  ) derives Decoder

  final case class Humidity(
    timeSeriesType: String,
    valueType: String,
    percentageUnit: String,
    min: Double,
    max: Double,
    dataPoints: Vector[HumidityDataPoint],
  ) derives Decoder

  final case class HumidityDataPoint(
    timestamp: Instant,
    value: Double,
  ) derives Decoder

  final case class InsideTemperature(
    timeSeriesType: String,
    valueType: String,
    min: Max,
    max: Max,
    dataPoints: Vector[InsideTemperatureDataPoint],
  ) derives Decoder

  final case class InsideTemperatureDataPoint(
    timestamp: Instant,
    value: Max,
  ) derives Decoder

  final case class Max(
    celsius: Double,
    fahrenheit: Double,
  ) derives Decoder

  final case class ReportSettings(
    timeSeriesType: String,
    valueType: String,
    dataIntervals: Vector[SettingsDataInterval],
  ) derives Decoder

  final case class SettingsDataInterval(
    from: String,
    to: String,
    value: SettingClass,
  ) derives Decoder

  final case class SettingClass(
    `type`: String,
    power: String,
    temperature: Max,
  ) derives Decoder

  final case class Stripes(
    timeSeriesType: String,
    valueType: String,
    dataIntervals: Vector[StripesDataInterval],
  ) derives Decoder

  final case class StripesDataInterval(
    from: String,
    to: String,
    value: PurpleValue,
  ) derives Decoder

  final case class PurpleValue(
    stripeType: String,
    setting: SettingClass,
  ) derives Decoder

  final case class Weather(
    condition: Condition,
    sunny: HotWaterProduction,
    slots: Slots,
  ) derives Decoder

  final case class Condition(
    timeSeriesType: String,
    valueType: String,
    dataIntervals: Vector[ConditionDataInterval],
  ) derives Decoder

  final case class ConditionDataInterval(
    from: String,
    to: String,
    value: Slot,
  ) derives Decoder

  final case class Slot(
    state: String,
    temperature: Max,
  ) derives Decoder

  final case class Slots(
    timeSeriesType: String,
    valueType: String,
    slots: Map[String, Slot],
  ) derives Decoder
