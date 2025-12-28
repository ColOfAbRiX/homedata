package com.colofabrix.scala.tado4s

import io.circe.*
import io.circe.derivation.*

class Tado4sError(message: String, inner: Option[Throwable] = None) extends Throwable(message) {
  inner.foreach(super.addSuppressed)
}

final case class TadoErrorResponse(errors: List[TadoError])
  extends Tado4sError(errors.mkString("'", ", ", "'"))
  derives Decoder {
  override def toString(): String = errors.mkString("'", ", ", "'")
}

final case class TadoError(code: String, title: String) derives Decoder {
  override def toString(): String = s"$code - $title"
}
