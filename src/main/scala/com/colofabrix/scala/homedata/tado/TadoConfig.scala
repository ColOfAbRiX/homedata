package com.colofabrix.scala.homedata.tado

import com.colofabrix.scala.tado4s.store.TadoRefreshToken
import pureconfig.*

final case class TadoConfig(
  requestsPerSec: Double,
  initialRefreshToken: TadoRefreshToken,
) derives ConfigReader

object TadoConfig {

  val config =
    ConfigSource
      .default
      .at("homedata.tado")
      .loadOrThrow[TadoConfig]

}
