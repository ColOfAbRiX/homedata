package com.colofabrix.scala.tado4s

final class Tado4sError(message: String, inner: Option[Throwable] = None) extends Throwable(message):
  inner.foreach(super.addSuppressed)
