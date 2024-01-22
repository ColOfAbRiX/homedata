package com.colofabrix.scala.tado4s

import cats.effect.*
import java.time.LocalDate

object Main extends IOApp.Simple:

  val run =
    for
      tadoClient <- Tado4sClient.clientF[IO]()
      account    <- tadoClient.getAccountInfo()
      myHome      = account.homes.head
      // firstHome  <- tadoClient.getHomeDetails(myHome.id)
      // homeState  <- tadoClient.getHomeState(myHome.id)
      zones      <- tadoClient.getHomeZones(myHome.id)
      aZone       = zones.head
      // zone2State <- tadoClient.getZoneState(myHome.id, aZone.id)
      // weather    <- tadoClient.getHomeWeather(myHome.id)
      dayReport  <- tadoClient.getDayReport(myHome.id, aZone.id, LocalDate.now())
      _          <- tadoClient.logout()
    yield
      // println(firstHome)
      // println("")
      println(zones)
      println("")
      // println(homeState)
      // println("")
      // println(zone2State)
      // println("")
      // println(weather)
      println("")
      println(dayReport)
