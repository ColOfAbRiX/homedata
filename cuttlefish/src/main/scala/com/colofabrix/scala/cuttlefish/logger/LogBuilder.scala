package com.colofabrix.scala.cuttlefish.logger

import cats.effect.Async
import cats.syntax.all.*
import org.http4s.Charset
import org.http4s.Headers
import org.http4s.MediaType
import org.http4s.Message
import org.typelevel.ci.CIString
import scodec.bits.ByteVector

private[logger] object LogBuilder:

  def logHeaders[F[_]](message: Message[F], logLevel: LogLevel, redactSensitiveHeaders: Boolean): String =
    val redactHeadersWhen: CIString => Boolean =
      if redactSensitiveHeaders then Headers.SensitiveHeaders.contains
      else _ => false

    if logLevel < LogLevel.Debug then
      ""
    else
      message
        .headers
        .mkString("Headers(", ", ", ")", redactHeadersWhen)

  def logBody[F[_]: Async](message: Message[F], logLevel: LogLevel): F[String] =
    if logLevel < LogLevel.Trace then
      Async[F].pure("")
    else
      val isBinary = message.contentType.exists(_.mediaType.binary)

      val isJson =
        message
          .contentType
          .exists { mT =>
            mT.mediaType == MediaType.application.json || mT.mediaType.subType.endsWith("+json")
          }

      if (!isBinary || isJson)
        message
          .bodyText(implicitly, message.charset.getOrElse(Charset.`UTF-8`))
          .compile
          .string
          .map(text => s"""body="$text"""")
      else
        message
          .body
          .compile
          .to(ByteVector)
          .map(_.toHex)
          .map(text => s"""body="$text"""")
