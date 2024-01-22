package com.colofabrix.scala.tado4s

import cats.effect.*

object Main extends IOApp.Simple:

  val run =
    for
      tadoClient <- Tado4sClient.clientF[IO]()
      _          <- tadoClient.login("", "")
      account    <- tadoClient.getAccountInfo()
      myHomeId    = account.homes.head.id
      firstHome  <- tadoClient.getHomeDetails(myHomeId)
      zones      <- tadoClient.getHomeZones(myHomeId)
      _          <- tadoClient.logout()
    yield
      println(firstHome)
      println("")
      println(zones)
