package com.colofabrix.scala.homedata.tado

import com.colofabrix.scala.tado4s.store.TadoRefreshToken
import pureconfig.*
import pureconfig.generic.derivation.default.*

final case class TadoConfig(
  requestsPerSec: Double,
  initialRefreshToken: TadoRefreshToken,
) derives ConfigReader

object TadoConfig {

  val config =
    ConfigSource
      .default
      .at("tado")
      .loadOrThrow[TadoConfig]

}
