package com.colofabrix.scala.homedata.tado.readings

import com.colofabrix.scala.tado4s.api.DayReportResponse
import io.circe.parser.{ decode => circeDecode }
import org.scalatest.freespec.AnyFreeSpecLike
import org.scalatest.matchers.should.Matchers
import scala.io.Source

class ReportConverterSpec extends AnyFreeSpecLike with Matchers:

  "ReportConverter should convert the json" in {
    ReportConverter.convert("Room #1", sampleReport)
  }

  private lazy val sampleReport: DayReportResponse =
    val json =
      Source
        .fromResource("day_report_response.json")
        .getLines
        .mkString

    circeDecode[DayReportResponse](json).fold(error => throw error, identity)
