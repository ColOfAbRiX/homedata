package com.colofabrix.scala.tado4s.api

import com.colofabrix.scala.tado4s.api.DayReportResponse.*
import enumeratum.*
import enumeratum.EnumEntry.UpperSnakecase
import io.circe.Decoder
import io.circe.derivation.Configuration
import java.time.*

/**
 * DayReportResponse
 *
 * @param interval
 *   It's the interval of time that the report contains
 * @param stripes
 *   Stripes are the colored bands at the bottom of a graph, they represent the overall state of Tado, for example if
 *   the person is Away, Manual mode, Window, ...
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

  //  Measure  //

  transparent trait Measure:
    def timeSeriesType: String
    def valueType: String

  object Measure:

    final case class DataPoints[A](
      timeSeriesType: String,
      valueType: String,
      min: Option[A],
      max: Option[A],
      percentageUnit: Option[String],
      dataPoints: Vector[TimeSeriesType.DataPoints[A]],
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

  //  TimeSeriesType  //

  transparent trait TimeSeriesType[A]:
    def value: A

  object TimeSeriesType:

    final case class DataIntervals[A](
      from: OffsetDateTime,
      to: OffsetDateTime,
      value: A,
    ) extends TimeSeriesType[A] derives Decoder

    final case class DataPoints[A](
      timestamp: OffsetDateTime,
      value: A,
    ) extends TimeSeriesType[A] derives Decoder

  //  ValueType  //

  transparent trait ValueType

  object ValueType:

    //  Primitive Types  //

    type Percentage = Double

    type Bool = Boolean

    //  Groups  //

    final case class Stripes(
      stripeType: String,
      setting: Option[HeatingSetting],
    ) extends ValueType derives Decoder

    final case class Temperature(
      celsius: Double,
      fahrenheit: Double,
    ) extends ValueType derives Decoder

    final case class WeatherCondition(
      state: OutsideState,
      temperature: Temperature,
    ) extends ValueType derives Decoder

    final case class HeatingSetting(
      `type`: String,
      power: String,
      temperature: Option[Temperature],
    ) extends ValueType derives Decoder

    //  CallForHeat  //

    sealed abstract class CallForHeat(val dbValue: Int) extends ValueType with EnumEntry with UpperSnakecase

    object CallForHeat extends Enum[CallForHeat] with CirceEnum[CallForHeat]:

      case object None   extends CallForHeat(0)
      case object Low    extends CallForHeat(1)
      case object Medium extends CallForHeat(2)
      case object High   extends CallForHeat(3)

      val values = findValues

  end ValueType

  //  Other  //

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

  //  OutsideState  //

  sealed abstract class OutsideState(val dbValue: Int) extends EnumEntry with UpperSnakecase

  object OutsideState extends Enum[OutsideState] with CirceEnum[OutsideState]:

    case object Cloudy        extends OutsideState(1)
    case object CloudyMostly  extends OutsideState(2)
    case object CloudyPartly  extends OutsideState(3)
    case object Drizzle       extends OutsideState(4)
    case object NightClear    extends OutsideState(5)
    case object NightCloudy   extends OutsideState(6)
    case object Rain          extends OutsideState(7)
    case object ScatteredRain extends OutsideState(8)
    case object Sun           extends OutsideState(9)
    case object Foggy         extends OutsideState(10)
    case object Thunderstorm  extends OutsideState(11)

    val values = findValues
