package com.colofabrix.scala.timeflux.model

opaque type LineProtocolValue = String

object LineProtocolValue {

  extension (self: LineProtocolValue) {
    def value: String =
      self
  }

  def apply(value: String): LineProtocolValue =
    value

}
