package com.colofabrix.scala.timeflux.model

opaque type AuthToken = String

object AuthToken:

  extension (self: AuthToken) def value: String =
    self
  def apply(value: String): AuthToken =
    value

opaque type OrgId = String

object OrgId:

  extension (self: OrgId) def value: String =
    self
  def apply(value: String): OrgId =
    value

