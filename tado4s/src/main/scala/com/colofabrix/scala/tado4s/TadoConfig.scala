package com.colofabrix.scala.tado4s

import io.github.arainko.ducktape.*
import org.http4s.Uri
import pureconfig.*
import pureconfig.generic.derivation.default.*

private[tado4s] final case class TadoConfig(
  apiBase: Uri,
  apiAuth: Uri,
  clientSecret: String,
)

object TadoConfig:

  private[this] case class TadoReaderConfig(
    apiBase: String,
    apiAuth: String,
    clientSecret: String,
  ) derives ConfigReader

  val config =
    ConfigSource
      .default
      .at("tado")
      .loadOrThrow[TadoReaderConfig]
      .into[TadoConfig]
      .transform(
        Field.computed(_.apiBase, c => Uri.unsafeFromString(c.apiBase)),
        Field.computed(_.apiAuth, c => Uri.unsafeFromString(c.apiAuth)),
      )
