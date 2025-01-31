package com.colofabrix.scala.timeflux.model

private[timeflux] trait OrgIdHandler[A]:

  def get(value: A): Option[String]
  def set(value: A, orgID: Option[String]): A
