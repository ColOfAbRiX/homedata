package com.colofabrix.scala.timeflux

import cats.effect.Async
import cats.effect.std.AtomicCell
import cats.implicits.given
import com.colofabrix.scala.timeflux.api.*
import com.colofabrix.scala.timeflux.config.*
import com.colofabrix.scala.timeflux.model.*
import com.colofabrix.scala.timeflux.TimefluxClient.*
import org.http4s.*
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

  def login(orgId: OrgId, token: AuthToken): F[Unit] =
    logger.debug("Login") >>
    setCredentials(orgId, token) >>
    logger.trace("Logged in!")

  def logout(): F[Unit] =
    logger.debug("Logout") >>
    clearCredentials() >>
    clearAuthenticatedClient() >>
    logger.trace("Logged out!")

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
      TimefluxAuthenticatedClient[F](credentials.token, credentials.orgId):
        httpClient

  //  State management  //

  private def getCredentials(): F[TimefluxCredentials] =
    atomicState.get.flatMap:
      _.credentials match {
        case None =>
          TimefluxException("Required OrgID parameter is not set.", None).raiseError
        case Some(credentials) =>
          credentials.pure[F]
      }

  private def setCredentials(orgId: OrgId, token: AuthToken): F[Unit] =
    atomicState.update:
      _.copy(credentials = Some(TimefluxCredentials(token, orgId)))

  private def clearCredentials(): F[Unit] =
    atomicState.update:
      _.copy(credentials = None)

  private def atomicallyModifyAuthenticatedClient(f: Option[Client[F]] => F[Client[F]]): F[Client[F]] =
    atomicState.evalModify: state =>
      f(state.authenticatedClient).map: newAuthenticatedClient =>
        (state.copy(authenticatedClient = Some(newAuthenticatedClient)), newAuthenticatedClient)

  private def clearAuthenticatedClient(): F[Unit] =
    atomicState.update:
      _.copy(authenticatedClient = None)
