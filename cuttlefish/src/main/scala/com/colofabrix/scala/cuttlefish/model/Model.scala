package com.colofabrix.scala.cuttlefish.model

opaque type MeterPointNumber = String

object MeterPointNumber:

  extension (self: MeterPointNumber) def value: String =
    self
  def apply(value: String): MeterPointNumber =
    value

opaque type SerialNumber = String

object SerialNumber:

  extension (self: SerialNumber) def value: String =
    self
  def apply(value: String): SerialNumber =
    value

