package com.colofabrix.scala.cuttlefish.model

// MeterPointNumber

opaque type Throttle = Double

object Throttle {

  extension (self: Throttle)
    def value: Double =
      self
  def apply(value: Double): Throttle =
    value

}

// MeterPointNumber

opaque type MeterPointNumber = String

object MeterPointNumber {

  extension (self: MeterPointNumber)
    def value: String =
      self
  def apply(value: String): MeterPointNumber =
    value

}

// SerialNumber

opaque type SerialNumber = String

object SerialNumber {

  extension (self: SerialNumber)
    def value: String =
      self
  def apply(value: String): SerialNumber =
    value

}

// AccountNumber

opaque type AccountNumber = String

object AccountNumber {

  extension (self: AccountNumber)
    def value: String =
      self
  def apply(value: String): AccountNumber =
    value

}

// ProductCode

opaque type ProductCode = String

object ProductCode {

  extension (self: ProductCode)
    def value: String =
      self
  def apply(value: String): ProductCode =
    value

}

// TariffCode

opaque type TariffCode = String

object TariffCode {

  extension (self: TariffCode)
    def value: String =
      self
  def apply(value: String): TariffCode =
    value

}

// Postcode

opaque type Postcode = String

object Postcode {

  extension (self: Postcode)
    def value: String =
      self
  def apply(value: String): Postcode =
    value

}

// Mpan

opaque type Mpan = String

object Mpan {

  extension (self: Mpan)
    def value: String =
      self
  def apply(value: String): Mpan =
    value

}
