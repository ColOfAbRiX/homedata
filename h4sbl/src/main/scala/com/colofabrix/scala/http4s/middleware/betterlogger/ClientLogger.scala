package com.colofabrix.scala.http4s.middleware.betterlogger

import cats.*
import cats.effect.*
import cats.syntax.all.*
import fs2.*
import org.http4s.*
import org.http4s.client.*
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

/**
 * HTTP4s client middleware that logs requests and responses.
 *
 * This is a cleaner, more modular implementation that:
 * - Separates configuration from logic
 * - Uses a linear flow: request → capture → log → response
 * - Provides configurable colors and formatting
 */
object ClientLogger:

  /**
   * Creates a logging middleware for an HTTP4s client.
   *
   * @param config Configuration for logging behavior
   * @return A function that wraps a Client with logging
   */
  def apply[F[_]: Async](config: LogConfig = LogConfig.default)(client: Client[F]): Client[F] =
    Client { request =>
      Resource.suspend {
        for
          logger   <- Slf4jLogger.create[F]
          logLevel <- detectLogLevel(logger)
          result   <- wrapWithLogging(client, request, config, logger, logLevel)
        yield result
      }
    }

  // --- Log Level Detection ---

  private def detectLogLevel[F[_]: Monad](logger: SelfAwareStructuredLogger[F]): F[LogLevel] =
    (logger.isTraceEnabled, logger.isDebugEnabled, logger.isInfoEnabled, logger.isWarnEnabled, logger.isErrorEnabled)
      .mapN { (trace, debug, info, warn, error) =>
        if trace then LogLevel.Trace
        else if debug then LogLevel.Debug
        else if info then LogLevel.Info
        else if warn then LogLevel.Warn
        else if error then LogLevel.Error
        else LogLevel.Off
      }

  // --- Main Logging Logic ---

  private def wrapWithLogging[F[_]: Async](
    client: Client[F],
    request: Request[F],
    config: LogConfig,
    logger: SelfAwareStructuredLogger[F],
    logLevel: LogLevel,
  ): F[Resource[F, Response[F]]] =
    if logLevel < LogLevel.Debug then
      // Skip logging entirely if log level is too low
      Async[F].pure(client.run(request))
    else
      for
        hasLoggedRequest  <- Ref[F].of(false)
        hasLoggedResponse <- Ref[F].of(false)
        requestChunks     <- Ref[F].of(Vector.empty[Chunk[Byte]])
        responseChunks    <- Ref[F].of(Vector.empty[Chunk[Byte]])
      yield
        val logRequest  = logOnce(hasLoggedRequest, logRequestMessage(request, requestChunks, config, logger, logLevel))
        val logResponse = (resp: Response[F]) =>
          logOnce(hasLoggedResponse, logResponseMessage(resp, responseChunks, config, logger, logLevel))

        val capturedRequest = captureBody(request, requestChunks, logRequest)

        client
          .run(capturedRequest)
          .evalMap(response => captureResponseBody(response, responseChunks, logResponse(response)))
          .onFinalize(logRequest >> logResponse(Response[F]()).void)

  // --- Body Capture ---

  private def captureBody[F[_]: Async](
    request: Request[F],
    chunks: Ref[F, Vector[Chunk[Byte]]],
    onFinalize: F[Unit],
  ): Request[F] =
    val chunkSink: Pipe[F, Byte, Nothing] =
      _.chunks.flatMap(chunk => Stream.exec(chunks.update(_ :+ chunk)))

    val capturingPipe: Pipe[F, Byte, Byte] = stream =>
      stream
        .observe(chunkSink)
        .onFinalizeWeak(onFinalize)

    request.withBodyStream(capturingPipe(request.body))

  private def captureResponseBody[F[_]: Async](
    response: Response[F],
    chunks: Ref[F, Vector[Chunk[Byte]]],
    onFinalize: F[Unit],
  ): F[Response[F]] =
    val chunkSink: Pipe[F, Byte, Nothing] =
      _.chunks.flatMap(chunk => Stream.exec(chunks.update(_ :+ chunk)))

    val capturingPipe: Pipe[F, Byte, Byte] = stream =>
      stream
        .observe(chunkSink)
        .onFinalizeWeak(onFinalize)

    Async[F].pure(response.withBodyStream(capturingPipe(response.body)))

  // --- Logging Helpers ---

  private def logOnce[F[_]: Async](hasLogged: Ref[F, Boolean], action: F[Unit]): F[Unit] =
    hasLogged.getAndSet(true).flatMap {
      case true  => Async[F].unit
      case false => action
    }

  private def logRequestMessage[F[_]: Async](
    request: Request[F],
    chunks: Ref[F, Vector[Chunk[Byte]]],
    config: LogConfig,
    logger: SelfAwareStructuredLogger[F],
    logLevel: LogLevel,
  ): F[Unit] =
    for
      body    <- reconstructBody(chunks)
      message  = formatRequest(request, body, config, logLevel)
      _       <- logAtMaxLevel(logger, message, logLevel)
    yield ()

  private def logResponseMessage[F[_]: Async](
    response: Response[F],
    chunks: Ref[F, Vector[Chunk[Byte]]],
    config: LogConfig,
    logger: SelfAwareStructuredLogger[F],
    logLevel: LogLevel,
  ): F[Unit] =
    for
      body    <- reconstructBody(chunks)
      message  = formatResponse(response, body, config, logLevel)
      _       <- logAtMaxLevel(logger, message, logLevel)
    yield ()

  private def reconstructBody[F[_]: Async](chunks: Ref[F, Vector[Chunk[Byte]]]): F[String] =
    chunks.get.map { chunkVector =>
      val bytes = chunkVector.flatMap(_.toVector).toArray
      if bytes.isEmpty then ""
      else new String(bytes, "UTF-8")
    }

  private def logAtMaxLevel[F[_]](logger: SelfAwareStructuredLogger[F], message: String, level: LogLevel): F[Unit] =
    level match
      case LogLevel.Trace => logger.trace(message)
      case LogLevel.Debug => logger.debug(message)
      case LogLevel.Info  => logger.info(message)
      case LogLevel.Warn  => logger.warn(message)
      case LogLevel.Error => logger.error(message)
      case LogLevel.Off   => logger.debug(message)

  // --- Message Formatting ---

  private def formatRequest[F[_]](
    request: Request[F],
    body: String,
    config: LogConfig,
    logLevel: LogLevel,
  ): String =
    val c = config.colors
    val methodColor = if request.method.isSafe then c.safeMethod else c.unsafeMethod

    val parts = List(
      Some(s"${c.httpVersion}${request.httpVersion}${c.reset}"),
      Some(s"$methodColor${request.method}${c.reset}"),
      Some(s"${c.uri}${request.uri}${c.reset}"),
      formatHeaders(request.headers, config, logLevel),
      formatBody(body, config.logRequestBody, logLevel).map(b => s"${c.body}$b${c.reset}"),
    ).flatten

    parts.mkString(" ")

  private def formatResponse[F[_]](
    response: Response[F],
    body: String,
    config: LogConfig,
    logLevel: LogLevel,
  ): String =
    val c = config.colors
    val statusColor = response.status.responseClass match
      case Status.Informational | Status.Successful | Status.Redirection => c.successStatus
      case Status.ClientError => c.clientErrorStatus
      case Status.ServerError => c.serverErrorStatus

    val parts = List(
      Some(s"${c.httpVersion}${response.httpVersion}${c.reset}"),
      Some(s"$statusColor${response.status}${c.reset}"),
      formatHeaders(response.headers, config, logLevel),
      formatBody(body, config.logResponseBody, logLevel).map(b => s"${c.body}$b${c.reset}"),
    ).flatten

    parts.mkString(" ")

  private def formatHeaders(headers: Headers, config: LogConfig, logLevel: LogLevel): Option[String] =
    if logLevel < LogLevel.Debug then None
    else
      val redactWhen: org.typelevel.ci.CIString => Boolean =
        if config.redactHeaders then Headers.SensitiveHeaders.contains
        else _ => false

      val formatted = headers.mkString("Headers(", ", ", ")", redactWhen)
      Some(s"${config.colors.headers}$formatted${config.colors.reset}")

  private def formatBody(body: String, shouldLog: Boolean, logLevel: LogLevel): Option[String] =
    if !shouldLog || logLevel < LogLevel.Trace || body.isEmpty then None
    else Some(s"""body="$body"""")
