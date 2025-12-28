package com.colofabrix.scala.homedata.tado.readings

import com.colofabrix.scala.tado4s.api.*
import io.circe.parser.{ decode => circeDecode }
import org.scalatest.EitherValues
import org.scalatest.freespec.AnyFreeSpecLike
import org.scalatest.matchers.should.Matchers
import scala.io.Source
import java.time.OffsetDateTime

class TadoApiDecoders extends AnyFreeSpecLike with Matchers with EitherValues {

  "Circe should decode a sample file" - {

    "for DayReportResponse" in {
      val files =
        List(
          "day_report_response/sample_1.jsonc",
          "day_report_response/sample_2.jsonc",
          "day_report_response/sample_3.jsonc",
          "day_report_response/sample_4.jsonc",
          "day_report_response/sample_5.jsonc",
        )

      files.foreach { jsonFile =>
        withClue(s"Working with $jsonFile: ") {
          val actual = circeDecode[DayReportResponse](loadJson(jsonFile))
          actual should matchPattern { case Right(_) => }
        }
      }
    }

    "for AccountResponse" in {
      val json   = loadJson("account_response.json")
      val actual = circeDecode[AccountResponse](json)
      actual should matchPattern { case Right(_) => }
    }

    "for HomeResponse" in {
      val json   = loadJson("home_response.json")
      val actual = circeDecode[HomeResponse](json)
      actual should matchPattern { case Right(_) => }
    }

    "for HomeStateResponse" in {
      val json   = loadJson("home_state_response.json")
      val actual = circeDecode[HomeStateResponse](json)
      actual should matchPattern { case Right(_) => }
    }

    "for HomeDeviceResponse" in {
      val json   = loadJson("home_devices.json")
      val actual = circeDecode[List[HomeDeviceResponse]](json)
      actual should matchPattern { case Right(_) => }
    }

    "for HomeUserResponse" in {
      val json   = loadJson("home_users.json")
      val actual = circeDecode[List[HomeUserResponse]](json)
      actual should matchPattern { case Right(_) => }
    }

    "for HomeZoneResponse" in {
      val json   = loadJson("home_zones_response.json")
      val actual = circeDecode[List[HomeZoneResponse]](json)
      actual should matchPattern { case Right(_) => }
    }

    "for WeatherResponse" in {
      val json   = loadJson("weather_response.json")
      val actual = circeDecode[WeatherResponse](json)
      actual should matchPattern { case Right(_) => }
    }

    "for ZoneStateResponse" in {
      val json   = loadJson("zone_state_response.json")
      val actual = circeDecode[ZoneStateResponse](json)
      actual should matchPattern { case Right(_) => }
    }

  }

  private def loadJson(file: String): String =
    Source
      .fromResource(s"sample_api/$file")
      .getLines
      .map(_.replaceAll("\\s*//.*$", ""))
      .mkString

}
