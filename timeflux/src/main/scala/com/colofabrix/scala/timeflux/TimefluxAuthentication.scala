package com.colofabrix.scala.timeflux

import cats.effect.Async
import cats.effect.std.AtomicCell
import cats.implicits.given
import com.colofabrix.scala.timeflux.api.*
import com.colofabrix.scala.timeflux.config.*
import com.colofabrix.scala.timeflux.TimefluxClient.*
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.client.middleware.*
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

/**
 * InfluxDB Authentication
 */
final private[timeflux] class TimefluxAuthentication[F[_]: Async](
  httpClient: Client[F],
  config: TimefluxConfig,
  atomicState: AtomicCell[F, TimefluxClientState[F]],
) extends Http4sClientDsl[F]:

  implicit private val logger: SelfAwareStructuredLogger[F] =
    Slf4jLogger.getLogger[F]

  def withAuthClient[A](): F[Client[F]] =
    atomicallyModifyAuthenticatedClient:
      case None =>
        for
          credentials <- getCredentials()
          client      <- buildHttpClient(credentials).pure[F]
          _           <- logger.debug("Creating new Timeflux authenticated client")
        yield client
      case Some(client) =>
        logger.trace(s"Returning Timeflux authenticated client") >>
        client.pure[F]

  private def buildHttpClient(credentials: TimefluxCredentials): Client[F] =
    val retryPolicy =
      RetryPolicy[F](
        backoff = RetryPolicy.exponentialBackoff(config.maxRetryTime, config.maxRetries),
      )

    Retry(retryPolicy):
      TimefluxAuthenticatedClient[F](credentials.token):
        httpClient

  //  State management  //

  private def getCredentials(): F[TimefluxCredentials] =
    atomicState.get.flatMap:
      _.credentials match {
        case None =>
          TimefluxException("No credentials set.", None).raiseError
        case Some(credentials) =>
          credentials.pure[F]
      }

  private def atomicallyModifyAuthenticatedClient(f: Option[Client[F]] => F[Client[F]]): F[Client[F]] =
    atomicState.evalModify: state =>
      f(state.authenticatedClient).map: newAuthenticatedClient =>
        (state.copy(authenticatedClient = Some(newAuthenticatedClient)), newAuthenticatedClient)
