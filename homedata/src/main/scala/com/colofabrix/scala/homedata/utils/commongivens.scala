package com.colofabrix.scala.homedata.utils

import pureconfig.*
import scala.concurrent.duration.*
import org.http4s.Uri

package object commongivens:

  given ConfigReader[FiniteDuration] =
    ConfigReader.fromString:
      ConvertHelpers.optF: str =>
        Some(Duration(str)).collect { case fd: FiniteDuration => fd }

  given ConfigReader[Uri] =
    ConfigReader.fromString:
      ConvertHelpers.tryF: str =>
        Uri.fromString(str).toTry
