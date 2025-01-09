package com.colofabrix.scala.cuttlefish.model

// MeterPointNumber

opaque type Throttle = Double

object Throttle:

  extension (self: Throttle)
    def value: Double =
      self
  def apply(value: Double): Throttle =
    value

// MeterPointNumber

opaque type MeterPointNumber = String

object MeterPointNumber:

  extension (self: MeterPointNumber)
    def value: String =
      self
  def apply(value: String): MeterPointNumber =
    value

// SerialNumber

opaque type SerialNumber = String

object SerialNumber:

  extension (self: SerialNumber)
    def value: String =
      self
  def apply(value: String): SerialNumber =
    value
