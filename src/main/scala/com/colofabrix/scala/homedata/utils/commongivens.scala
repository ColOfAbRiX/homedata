package com.colofabrix.scala.homedata.utils

import java.time.OffsetDateTime
import org.http4s.Uri
import pureconfig.*
import scala.concurrent.duration.*
import scala.util.Try

package object commongivens {

  given ConfigReader[FiniteDuration] =
    ConfigReader.fromString:
      ConvertHelpers.optF: str =>
        Some(Duration(str)).collect { case fd: FiniteDuration => fd }

  given ConfigReader[Uri] =
    ConfigReader.fromString:
      ConvertHelpers.tryF: str =>
        Uri.fromString(str).toTry

  given ConfigReader[OffsetDateTime] =
    ConfigReader.fromString:
      ConvertHelpers.tryF: str =>
        Try(OffsetDateTime.parse(str))

}
