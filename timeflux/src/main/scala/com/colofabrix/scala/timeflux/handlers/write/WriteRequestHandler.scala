package com.colofabrix.scala.timeflux.handlers.write

import cats.effect.Async
import cats.implicits.given
import com.colofabrix.scala.timeflux.api.*
import com.colofabrix.scala.timeflux.config.*
import com.colofabrix.scala.timeflux.handlers.ErrorHandling.*
import com.colofabrix.scala.timeflux.measures.*
import com.colofabrix.scala.timeflux.model.*
import org.http4s.*
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.Method.*
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

final private[timeflux] class WriteRequestHandler[F[_]: Async](
  httpClient: Client[F],
  config: TimefluxConfig,
  baseApiUrl: Uri,
) extends Http4sClientDsl[F]:

  implicit private val logger: SelfAwareStructuredLogger[F] =
    Slf4jLogger.getLogger[F]

  def writeRequest(request: WriteRequest, values: fs2.Stream[F, Measure]): F[Unit] =
    request.batchWrites match {
      case Some(batchSize) =>
        logger.debug(s"Batching $batchSize InfluxDB measures into ${config.concurrentWrites} parallel writes") >>
        values
          .chunkN(batchSize, allowFewer = true)
          .parEvalMapUnordered(config.concurrentWrites) { chunk =>
            sendWriteRequest(baseApiUrl, request, fs2.Stream.chunk(chunk))
          }
          .compile
          .drain
      case None =>
        sendWriteRequest(baseApiUrl, request, values)
    }

  private def sendWriteRequest(baseApiUrl: Uri, request: WriteRequest, values: fs2.Stream[F, Measure]): F[Unit] =
    val headers =
      Headers(
        "Content-Type" -> "text/plain; charset=utf-8",
        "Accept"       -> "application/json",
      )

    val requestUri = (baseApiUrl / "write").withQueryParams(request.toQueryParams)

    val body =
      values
        .map(_.toLineProtocol(request.precision).value.trim + "\n")
        .evalTap(line => logger.trace(line.dropRight(1)))
        .through(fs2.text.utf8.encode)

    val http4sRequest =
      POST(
        uri = requestUri,
        body = body,
        headers = headers,
      )

    logger.debug(s"Called sendWriteRequest() with $request") >>
    httpClient
      .stream(http4sRequest)
      .flatMap(handleClientStreamError)
      .compile
      .drain
