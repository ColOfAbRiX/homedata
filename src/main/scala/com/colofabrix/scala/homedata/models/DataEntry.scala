package com.colofabrix.scala.homedata.models

import com.colofabrix.scala.timeflux.measures.Measure
import com.colofabrix.scala.homedata.scrape.ScrapeService
import java.time.OffsetDateTime

final case class DataEntry(
  measure: Measure,
  service: ScrapeService,
  timestamp: OffsetDateTime,
  entityId: String,
)
