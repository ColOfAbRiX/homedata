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
) extends Http4sClientDsl[F] {

  implicit private val logger: SelfAwareStructuredLogger[F] =
    Slf4jLogger.getLogger[F]

  /**
   * Get an authenticated HTTP client.
   */
  def withAuthClient(): F[Client[F]] =
    atomicState.evalModify { state =>
      state.authenticatedClient match
        case Some(client) =>
          logger.trace("Returning Timeflux authenticated client").as((state, client))
        case None =>
          state.credentials match
            case None =>
              TimefluxException("No credentials set.", None).raiseError[F, (TimefluxClientState[F], Client[F])]
            case Some(credentials) =>
              val newClient = buildHttpClient(credentials)
              val newState  = state.copy(authenticatedClient = Some(newClient))
              logger.debug("Creating new Timeflux authenticated client").as((newState, newClient))
    }

  private def buildHttpClient(credentials: TimefluxCredentials): Client[F] =
    val retryPolicy =
      RetryPolicy[F](
        backoff = RetryPolicy.exponentialBackoff(config.maxRetryTime, config.maxRetries),
      )

    Retry(retryPolicy):
      TimefluxAuthenticatedClient[F](credentials.token):
        httpClient

}
