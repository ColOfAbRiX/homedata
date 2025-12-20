package com.colofabrix.scala.timeflux.api

/**
 * List Organizations GET Request
 */
final case class ListOrgsRequest(org: Option[String], orgID: Option[String]):

  def toQueryParams: Map[String, String] =
    Map.empty[String, String] ++
    org.map("org" -> _) ++
    orgID.map("orgID" -> _)
