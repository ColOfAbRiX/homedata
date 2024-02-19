package com.colofabrix.scala.homedata.tado

import cats.*
import cats.implicits.given

final case class TadoRunningReading(
  atHome: Option[Boolean] = None,
  windowOpen: Option[Boolean] = None,
  temperature: Option[Double] = None,
  humidity: Option[Double] = None,
  outsideTemperature: Option[Double] = None,
  outsideState: Option[String] = None,
  outsideSun: Option[Boolean] = None,
  setTemperature: Option[Double] = None,
  heatingModulation: Option[Double] = None,
)

object TadoRunningReading:
  given Show[TadoRunningReading] with
    def show(t: TadoRunningReading): String =
      val fields =
        List(
          t.atHome.map(x => s"atHome=$x"),
          t.windowOpen.map(x => s"windowOpen=$x"),
          t.temperature.map(x => s"temperature=$x"),
          t.humidity.map(x => s"humidity=$x"),
          t.outsideTemperature.map(x => s"outsideTemperature=$x"),
          t.outsideState.map(x => s"outsideState=$x"),
          t.outsideSun.map(x => s"outsideSun=$x"),
          t.setTemperature.map(x => s"setTemperature=$x"),
          t.heatingModulation.map(x => s"heatingModulation=$x"),
        )
      fields.flattenOption.mkString("TadoRunningReading(", ", ", ")")

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
        outsideSun = (x.outsideSun, y.outsideSun).last,
        setTemperature = (x.setTemperature, y.setTemperature).avg,
        heatingModulation = (x.heatingModulation, y.heatingModulation).avg,
      )

  extension [A](self: (Option[A], Option[A]))
    def avg(using A: Fractional[A]): Option[A] =
      val list = self.toList.flatMap(_.toList)
      list.reduceOption(A.plus).map(A.div(_, A.fromInt(list.length)))

    def last: Option[A] =
      self._2
