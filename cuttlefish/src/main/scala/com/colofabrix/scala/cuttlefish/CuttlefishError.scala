package com.colofabrix.scala.cuttlefish

import io.circe.*
import io.circe.derivation.*

class CuttlefishError(message: String, inner: Option[Throwable] = None) extends Throwable(message):
  inner.foreach(super.addSuppressed)

final case class CuttlefishRequestError(
  message: String,
  body: String,
) extends CuttlefishError(message) derives Decoder
