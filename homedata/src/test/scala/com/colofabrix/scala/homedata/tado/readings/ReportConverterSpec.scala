package com.colofabrix.scala.homedata.tado.readings

import cats.effect.unsafe.implicits.given
import com.colofabrix.scala.homedata.tado.IOValues
import com.colofabrix.scala.tado4s.api.DayReportResponse
import io.circe.parser.{ decode => circeDecode }
import org.scalatest.freespec.AnyFreeSpecLike
import org.scalatest.matchers.should.Matchers
import scala.io.Source

class ReportConverterSpec extends AnyFreeSpecLike with Matchers with IOValues:

  "ReportConverter should convert the json" in {
    val result =
      ReportConverter
        .convert("Room #1", sampleReport)
        .result()
  }

  private lazy val sampleReport: DayReportResponse =
    val json =
      Source
        .fromResource("day_report_response.json")
        .getLines
        .mkString

    circeDecode[DayReportResponse](json).fold(error => throw error, identity)
