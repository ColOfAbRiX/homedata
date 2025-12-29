package com.colofabrix.scala.timeflux.model

import scala.collection.immutable.ListMap

opaque type ResultRow = ListMap[String, String]

object ResultRow {

  def apply(value: ListMap[String, String]): ResultRow =
    value

  extension (self: ResultRow) {

    def value: ListMap[String, String] =
      self

    def apply(key: String): String =
      self(key)

    def get(key: String): Option[String] =
      self.get(key)

    def keys: Iterable[String] =
      self.keys

    def values: Iterable[String] =
      self.values

  }

}
