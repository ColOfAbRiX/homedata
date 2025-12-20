package com.colofabrix.scala.timeflux.api

import io.circe.*
import io.circe.derivation.*

/**
 * Create Organization POST Request
 */
final case class CreateOrgRequest(
  name: String,
  description: Option[String],
) derives Encoder.AsObject
