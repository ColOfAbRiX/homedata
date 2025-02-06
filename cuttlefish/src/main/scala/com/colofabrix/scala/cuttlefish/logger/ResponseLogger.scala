package com.colofabrix.scala.cuttlefish.logger

import cats.effect.*
import cats.syntax.all.*
import com.colofabrix.scala.cuttlefish.logger.LogLevel.*
import fs2.*
import org.http4s.*
import org.http4s.client.*
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import scala.Console.*

final class ResponseLogger[F[_]](using F: Async[F]) {

  implicit private val logger: SelfAwareStructuredLogger[F] =
    Slf4jLogger.getLogger[F]

  private def spaced(x: String): String =
    if (x.isEmpty) x else s" $x"

  private def responseColor(response: Response[F]): String =
    response.status.responseClass match {
      case Status.Informational | Status.Successful | Status.Redirection => GREEN
      case Status.ClientError                                            => YELLOW
      case Status.ServerError                                            => RED
    }

  private def logMessage(redactHeaders: Boolean, logLevel: LogLevel, response: Response[F]): F[Unit] =
    val mainColor = responseColor(response)

    val prelude =
      s"$WHITE${response.httpVersion} $RESET" +
      s"$mainColor${response.status}$RESET"

    val headers = LogBuilder.logHeaders(response, logLevel, redactHeaders)

    LogBuilder
      .logBody(response, logLevel)
      .map { body =>
        s"$prelude$BLUE${spaced(headers)}$RESET$WHITE${spaced(body)}$RESET"
      }
      .flatMap(logger.logMax(_))
      .handleErrorWith(logger.error(_)("Error logging request body"))

  def log(client: Client[F], response: Response[F], redactHeaders: Boolean = true): F[Resource[F, Response[F]]] =
    logger
      .currentLogLevel()
      .flatMap {
        case logLevel if logLevel < LogLevel.Debug =>
          Resource.pure(response).pure[F]
        case logLevel =>
          Ref[F]
            .of(Vector.empty[Chunk[Byte]])
            .map { vector =>
              val dumpChunksToVec: Pipe[F, Byte, Nothing] =
                _.chunks.flatMap(s => Stream.exec(vector.update(_ :+ s)))

              val acquire = F.pure(response.withBodyStream(response.body.observe(dumpChunksToVec)))

              // Cannot Be Done Asynchronously - Otherwise All Chunks May Not Be Appended before Finalization
              Resource.make(acquire) { _ =>
                val newBody =
                  Stream
                    .eval(vector.get)
                    .flatMap(Stream.emits)
                    .unchunks

                logMessage(redactHeaders, logLevel, response.withBodyStream(newBody))
              }
            }
      }
}

object ResponseLogger:

  def apply[F[_]: Async](redactHeaders: Boolean = true)(client: Client[F]): Client[F] =
    Client { request =>
      client
        .run(request)
        .flatMap { response =>
          Resource.suspend {
            new ResponseLogger[F].log(client, response, redactHeaders)
          }
        }
    }
