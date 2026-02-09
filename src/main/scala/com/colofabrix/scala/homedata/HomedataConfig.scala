package com.colofabrix.scala.homedata

import com.colofabrix.scala.cuttlefish.model.*
import java.nio.file.Path
import pureconfig.*
import pureconfig.generic.derivation.default.*
import scala.concurrent.duration.*

final case class HomedataConfig(
  scrapeLog: ScrapeLogConfig,
  pollTime: FiniteDuration,
) derives ConfigReader

final case class ScrapeLogConfig(
  logPath: Path,
  batchSize: Int,
  batchWait: FiniteDuration,
) derives ConfigReader

object HomedataConfig {

  val config =
    ConfigSource
      .default
      .at("homedata")
      .loadOrThrow[HomedataConfig]

}
