package com.colofabrix.scala.timeflux.model

import cats.data.NonEmptyList

opaque type ResultRow = NonEmptyList[String]

object ResultRow:

  def apply(value: NonEmptyList[String]): ResultRow =
    value

  extension (self: ResultRow) {
    def value: NonEmptyList[String] =
      self
  }
