package com.colofabrix.scala.homedata.tado.readings

import cats.*
import cats.implicits.given
import com.colofabrix.scala.timeflux.measures.*
import java.time.OffsetDateTime

final case class TadoReading(
  time: Option[OffsetDateTime],
  room: Option[String],
  atHome: Option[Boolean],
  heatingModulation: Option[Double],
  humidity: Option[Double],
  isOff: Option[Boolean],
  manualSet: Option[Boolean],
  outsideState: Option[Int],
  outsideSun: Option[Boolean],
  outsideTemperature: Option[Double],
  setTemperature: Option[Double],
  temperature: Option[Double],
  windowOpen: Option[Boolean],
)

object TadoReading {

  def build(
    time: Option[OffsetDateTime] = None,
    room: Option[String] = None,
    atHome: Option[Boolean] = None,
    heatingModulation: Option[Double] = None,
    humidity: Option[Double] = None,
    isOff: Option[Boolean] = None,
    manualSet: Option[Boolean] = None,
    outsideState: Option[Int] = None,
    outsideSun: Option[Boolean] = None,
    outsideTemperature: Option[Double] = None,
    setTemperature: Option[Double] = None,
    temperature: Option[Double] = None,
    windowOpen: Option[Boolean] = None,
  ) = TadoReading(
    time,
    room,
    atHome,
    heatingModulation,
    humidity,
    isOff,
    manualSet,
    outsideState,
    outsideSun,
    outsideTemperature,
    setTemperature,
    temperature,
    windowOpen,
  )

  given TimefluxSerializable[TadoReading] with

    override def toMeasure(reading: TadoReading): Measure =
      val room = MeasureTag("room", reading.room.getOrElse("NO-ROOM"))

      val fields =
        Vector(
          reading.atHome.map(r => MeasureField("atHome", FieldValue(r))),
          reading.heatingModulation.map(r => MeasureField("heatingModulation", FieldValue(r))),
          reading.humidity.map(r => MeasureField("humidity", FieldValue(r))),
          reading.isOff.map(r => MeasureField("isOff", FieldValue(r))),
          reading.manualSet.map(r => MeasureField("manualSet", FieldValue(r))),
          reading.outsideState.map(r => MeasureField("outsideState", FieldValue(r))),
          reading.outsideSun.map(r => MeasureField("outsideSun", FieldValue(r))),
          reading.outsideTemperature.map(r => MeasureField("outsideTemperature", FieldValue(r))),
          reading.setTemperature.map(r => MeasureField("setTemperature", FieldValue(r))),
          reading.temperature.map(r => MeasureField("temperature", FieldValue(r))),
          reading.windowOpen.map(r => MeasureField("windowOpen", FieldValue(r))),
        )

      Measure(
        name = "tado",
        fields = fields.flattenOption,
        tags = Vector(room),
        time = reading.time.getOrElse(OffsetDateTime.MAX),
      )

  given Show[TadoReading] with

    def show(tadoReading: TadoReading): String =
      List
        .tabulate(tadoReading.productArity) { i =>
          val fieldName = tadoReading.productElementName(i)
          tadoReading.productElement(i) match {
            case Some(value) => s"$fieldName=$value"
            case None        => s"$fieldName=None"
            case x           => s"$fieldName=NOT_OPTION(${x.toString})"
          }
        }
        .mkString("TadoReading(", ", ", ")")

  given Monoid[TadoReading] with
    def empty: TadoReading =
      TadoReading.build()

    def combine(x: TadoReading, y: TadoReading): TadoReading =
      TadoReading(
        time = None,
        room = None,
        atHome = (x.atHome, y.atHome).readingsLast,
        heatingModulation = (x.heatingModulation, y.heatingModulation).readingsAvg,
        humidity = (x.humidity, y.humidity).readingsAvg,
        isOff = (x.isOff, y.isOff).readingsLast,
        manualSet = (x.manualSet, y.manualSet).readingsLast,
        outsideState = (x.outsideState, y.outsideState).readingsLast,
        outsideSun = (x.outsideSun, y.outsideSun).readingsLast,
        outsideTemperature = (x.outsideTemperature, y.outsideTemperature).readingsAvg,
        setTemperature = (x.setTemperature, y.setTemperature).readingsAvg,
        temperature = (x.temperature, y.temperature).readingsAvg,
        windowOpen = (x.windowOpen, y.windowOpen).readingsLast,
      )

  extension [A](self: (Option[A], Option[A])) {

    def readingsAvg(using A: Fractional[A]): Option[A] =
      val list = self._1.toList ::: self._2.toList
      list.reduceOption(A.plus).map(A.div(_, A.fromInt(list.length)))

    def readingsLast: Option[A] =
      self._1.orElse(self._2)

  }

}
