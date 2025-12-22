package com.colofabrix.scala.tado4s.store

import com.typesafe.config.{ConfigFactory, ConfigValueFactory}
import java.time.OffsetDateTime
import pureconfig.*
import pureconfig.generic.derivation.default.*

/**
 * Refresh token with issue time for token comparison
 */
final case class TadoRefreshToken(
  token: String,
  issueTime: OffsetDateTime,
) derives ConfigReader

object TadoRefreshToken:

  given ConfigReader[OffsetDateTime] =
    ConfigReader.fromString(s => Right(OffsetDateTime.parse(s)))

  given ConfigWriter[TadoRefreshToken] = (token: TadoRefreshToken) =>
    ConfigFactory
      .empty()
      .withValue("token", ConfigValueFactory.fromAnyRef(token.token))
      .withValue("issue-time", ConfigValueFactory.fromAnyRef(token.issueTime.toString))
      .root()
