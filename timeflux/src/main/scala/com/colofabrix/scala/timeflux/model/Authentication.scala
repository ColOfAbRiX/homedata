package com.colofabrix.scala.timeflux.model

opaque type AuthToken = String

object AuthToken {

  extension (self: AuthToken) def value: String =
    self
  def apply(value: String): AuthToken =
    value
  def unapply(authToken: AuthToken): Option[String] =
    Some(authToken)

}

opaque type OrgId = String

object OrgId {

  extension (self: OrgId) def value: String =
    self
  def apply(value: String): OrgId =
    value
  def unapply(orgId: OrgId): Option[String] =
    Some(orgId)

}

opaque type OrgName = String

object OrgName {

  extension (self: OrgName) def value: String =
    self
  def apply(value: String): OrgName =
    value
  def unapply(orgName: OrgName): Option[String] =
    Some(orgName)

}
