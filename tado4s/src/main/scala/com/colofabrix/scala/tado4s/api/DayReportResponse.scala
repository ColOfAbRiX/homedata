package com.colofabrix.scala.tado4s.api

import com.colofabrix.scala.tado4s.api.DayReportResponse.*
import enumeratum.*
import enumeratum.EnumEntry.UpperSnakecase
import io.circe.*
import java.time.*

/**
 * DayReportResponse
 */
final case class DayReportResponse(
  zoneType: ZoneType,
  interval: Interval,
  hoursInDay: Int,
  measuredData: MeasuredData,
  stripes: IntervalSeries[Stripes],
  settings: IntervalSeries[HeatingSetting],
  callForHeat: IntervalSeries[CallForHeat],
  hotWaterProduction: IntervalSeries[Boolean],
  weather: Weather,
) derives Decoder

object DayReportResponse:

  //  Time Series Containers  //

  final case class PointsSeries[A](
    timeSeriesType: String,
    valueType: String,
    min: Option[A],
    max: Option[A],
    percentageUnit: Option[String],
    dataPoints: Vector[DataPoint[A]],
  ) derives Decoder

  final case class IntervalSeries[A](
    timeSeriesType: String,
    valueType: String,
    dataIntervals: Vector[DataInterval[A]],
  ) derives Decoder

  final case class SlotMap[A](
    timeSeriesType: String,
    valueType: String,
    slots: Map[String, Option[A]],
  ) derives Decoder

  //  Time Series Elements  //

  final case class DataPoint[A](
    timestamp: OffsetDateTime,
    value: A,
  ) derives Decoder

  final case class DataInterval[A](
    from: OffsetDateTime,
    to: OffsetDateTime,
    value: Option[A],
  ) derives Decoder

  //  Value Types  //

  final case class Stripes(
    stripeType: StripeType,
    setting: Option[HeatingSetting],
  ) derives Decoder

  final case class Temperature(
    celsius: Double,
    fahrenheit: Double,
  ) derives Decoder

  final case class WeatherCondition(
    state: OutsideState,
    temperature: Temperature,
  ) derives Decoder

  final case class HeatingSetting(
    `type`: HeatingType,
    power: Power,
    temperature: Option[Temperature],
  ) derives Decoder

  //  Enums  //

  sealed abstract class ZoneType extends EnumEntry with UpperSnakecase

  object ZoneType extends Enum[ZoneType] with CirceEnum[ZoneType]:

    case object Heating  extends ZoneType
    case object HotWater extends ZoneType

    val values = findValues

  sealed abstract class StripeType extends EnumEntry with UpperSnakecase

  object StripeType extends Enum[StripeType] with CirceEnum[StripeType]:

    case object Home                        extends StripeType
    case object Away                        extends StripeType
    case object OpenWindowDetected          extends StripeType
    case object Manual                      extends StripeType
    case object OverlayActive               extends StripeType
    case object MeasuringDeviceDisconnected extends StripeType

    val values = findValues

  sealed abstract class HeatingType extends EnumEntry with UpperSnakecase

  object HeatingType extends Enum[HeatingType] with CirceEnum[HeatingType]:

    case object Heating  extends HeatingType
    case object HotWater extends HeatingType

    val values = findValues

  sealed abstract class Power extends EnumEntry with UpperSnakecase

  object Power extends Enum[Power] with CirceEnum[Power]:

    case object On  extends Power
    case object Off extends Power

    val values = findValues

  sealed abstract class CallForHeat(val dbValue: Int) extends EnumEntry with UpperSnakecase

  object CallForHeat extends Enum[CallForHeat] with CirceEnum[CallForHeat]:

    case object None   extends CallForHeat(0)
    case object Low    extends CallForHeat(1)
    case object Medium extends CallForHeat(2)
    case object High   extends CallForHeat(3)

    val values = findValues

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

  //  Composite Types  //

  final case class Interval(
    from: OffsetDateTime,
    to: OffsetDateTime,
  ) derives Decoder

  final case class MeasuredData(
    measuringDeviceConnected: IntervalSeries[Boolean],
    insideTemperature: PointsSeries[Temperature],
    humidity: PointsSeries[Double],
  ) derives Decoder

  final case class Weather(
    condition: IntervalSeries[WeatherCondition],
    sunny: IntervalSeries[Boolean],
    slots: SlotMap[WeatherCondition],
  ) derives Decoder
