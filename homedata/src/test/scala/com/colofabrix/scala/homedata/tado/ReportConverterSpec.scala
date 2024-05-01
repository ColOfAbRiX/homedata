package com.colofabrix.scala.homedata.tado

import cats.implicits.given
import com.colofabrix.scala.tado4s.api.DayReportResponse
import io.circe.parser.{ decode => circeDecode }
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers
import scala.io.Source

class ReportConverterSpec extends AnyFlatSpecLike with Matchers:

  "ReportConverter" should "load the report" in {
    ReportConverter.convert("Room #1", sampleReport)
  }

  private lazy val sampleReport: DayReportResponse =
    val json =
      Source
        .fromResource("day_report_response.json")
        .getLines
        .mkString

    circeDecode[DayReportResponse](json).fold(error => throw error, identity)
