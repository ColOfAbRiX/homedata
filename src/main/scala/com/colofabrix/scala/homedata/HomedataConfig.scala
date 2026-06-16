package com.colofabrix.scala.homedata

import com.colofabrix.scala.cuttlefish.models.*
import com.colofabrix.scala.homedata.utils.commongivens.given
import java.nio.file.Path
import java.time.OffsetDateTime
import pureconfig.*
import pureconfig.generic.derivation.default.*
import scala.concurrent.duration.*

final case class HomedataConfig(
  scrapeLog: ScrapeLogConfig,
  scrapeFromDate: OffsetDateTime,
  scrapeToDate: Option[OffsetDateTime],
  pollTime: Option[FiniteDuration],
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
