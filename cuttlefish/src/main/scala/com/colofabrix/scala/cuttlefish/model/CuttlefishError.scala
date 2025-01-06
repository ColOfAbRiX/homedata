package com.colofabrix.scala.cuttlefish.model

import io.circe.*
import io.circe.derivation.*

class CuttlefishError(message: String, inner: Option[Throwable] = None) extends Throwable(message):
  inner.foreach(super.addSuppressed)

final case class CuttlefishRequestError(detail: String) extends CuttlefishError(detail) derives Decoder
