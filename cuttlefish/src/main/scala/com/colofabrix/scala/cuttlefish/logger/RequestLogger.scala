package com.colofabrix.scala.cuttlefish.logger

import cats.effect.*
import cats.implicits.*
import com.colofabrix.scala.cuttlefish.logger.LogLevel.*
import fs2.*
import org.http4s.*
import org.http4s.client.*
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import scala.Console.*

final class RequestLogger[F[_]] private (using F: Async[F]):

  private val logger: SelfAwareStructuredLogger[F] =
    Slf4jLogger.getLogger[F]

  private def spaced(x: String): String =
    if (x.isEmpty) x else s" $x"

  private def logMessage(redactHeaders: Boolean, logLevel: LogLevel, request: Request[F]): F[Unit] =
    val methodColor =
      if (request.method.isSafe) GREEN
      else YELLOW

    val prelude =
      s"$WHITE${request.httpVersion} $RESET" +
      s"$methodColor${request.method} $RESET" +
      s"$MAGENTA$BOLD${request.uri}$RESET"

    val headers = LogBuilder.logHeaders(request, logLevel, redactHeaders)

    LogBuilder
      .logBody(request, logLevel)
      .map { body =>
        s"$prelude$BLUE${spaced(headers)}$RESET$WHITE${spaced(body)}$RESET"
      }
      .flatMap(logger.logMax(_))
      .handleErrorWith(logger.error(_)("Error logging request body"))

  def log(client: Client[F], request: Request[F], redactHeaders: Boolean = true): F[Resource[F, Response[F]]] =
    (F.ref(false), F.ref(Vector.empty[Chunk[Byte]]), logger.currentLogLevel()).mapN {
      case (_, _, logLevel) if logLevel < LogLevel.Debug =>
        client.run(request)
      case (hasLogged, vector, logLevel) =>
        val newBody =
          Stream
            .eval(vector.get)
            .flatMap(v => Stream.emits(v))
            .unchunks

        val logOnceAtEnd: F[Unit] =
          hasLogged
            .getAndSet(true)
            .ifM(
              F.unit,
              logMessage(redactHeaders, logLevel, request.withBodyStream(newBody)),
            )

        // Cannot Be Done Asynchronously - Otherwise All Chunks May Not Be Appended Previous to Finalization
        val logPipe: Pipe[F, Byte, Byte] =
          _.observe(_.chunks.flatMap(s => Stream.exec(vector.update(_ :+ s)))).onFinalizeWeak(logOnceAtEnd)

        // If the request body was not consumed (ex: bodiless GET) the second best we can do is log on the response body
        // finalizer the third best is on the response resource finalizer (ex: if the client failed to pull the body)
        client
          .run(request.withBodyStream(logPipe(request.body)))
          .map[Response[F]](response => response.withBodyStream(response.body.onFinalizeWeak(logOnceAtEnd)))
          .onFinalize(logOnceAtEnd)
    }

object RequestLogger:

  def apply[F[_]: Async](redactHeaders: Boolean = true)(client: Client[F]): Client[F] =
    Client { request =>
      Resource.suspend {
        new RequestLogger[F].log(client, request, redactHeaders)
      }
    }
