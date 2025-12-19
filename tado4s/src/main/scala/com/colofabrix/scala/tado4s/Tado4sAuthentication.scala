package com.colofabrix.scala.tado4s

import cats.effect.Async
import cats.effect.std.AtomicCell
import cats.implicits.given
import com.colofabrix.scala.tado4s.api.*
import com.colofabrix.scala.tado4s.security.*
import com.colofabrix.scala.tado4s.Tado4sClient.*
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import org.http4s.*
import org.http4s.circe.CirceEntityDecoder.*
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.client.middleware.{ Retry, RetryPolicy }
import org.http4s.Method.*
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

final private[tado4s] class Tado4sAuthentication[F[_]: Async](
  httpClient: Client[F],
  config: TadoConfig,
  atomicState: AtomicCell[F, TadoClientState[F]],
) extends Http4sClientDsl[F]:

  implicit private val logger: SelfAwareStructuredLogger[F] =
    Slf4jLogger.getLogger[F]

  def initialize(): F[Unit] =
    config.refreshToken match {
      case Some(token) =>
        atomicState.update(_.copy(refreshToken = Some(token)))
      case None =>
        Async[F].unit
    }

  def login(refreshToken: String): F[Unit] =
    logger.debug("Login with refresh token") >>
    atomicState.update(_.copy(refreshToken = Some(refreshToken), authToken = None, authenticatedClient = None))

  def logout(): F[Unit] =
    logger.debug("Logout") >>
    atomicState.update:
      _.copy(refreshToken = None, authToken = None, authenticatedClient = None)

  def withAuthClient[A](retries: Int = 1): F[Client[F]] =
    getAuthToken().flatMap {
      case None =>
        logger.debug("No auth token, attempting to get one via refresh token") >>
        handleTokenRefresh() >>
        withAuthClient(retries - 1)
      case Some(authToken) if isTokenExpired(authToken) =>
        logger.info("Tado token expired") >>
        logger.debug(s"Expired token: $authToken") >>
        clearAuthenticatedClient() >>
        handleTokenRefresh() >>
        withAuthClient(retries - 1)
      case Some(authToken) =>
        atomicallyModifyAuthenticatedClient {
          case Some(client) =>
            logger.debug(s"Returning Tado authenticated client with token $authToken") >>
            client.pure[F]
          case None =>
            for
              client <- buildHttpClient(authToken).pure[F]
              _      <- logger.debug(s"Creating Tado authenticated client with token $authToken")
            yield client
        }
    }

  private def handleTokenRefresh(): F[Unit] =
    atomicState
      .evalUpdate {
        case state @ TadoClientState(Some(refreshToken), _, _) =>
          refreshRequest(refreshToken).map { case (newAuthToken, newRefreshToken) =>
            state.copy(authToken = Some(newAuthToken), refreshToken = Some(newRefreshToken))
          }
        case TadoClientState(None, _, _) =>
          Tado4sError("No refresh token provided. Please run the device authorization flow to obtain one.").raiseError
      }
      .handleErrorWith { error =>
        val tadoError = Tado4sError("Error while refreshing API token", Some(error))
        logger.error(tadoError)("Error while refreshing API token") >>
        tadoError.raiseError
      }

  private def refreshRequest(refreshToken: String): F[(TadoAuthToken, String)] =
    for
      _ <- logger.info("Refreshing authentication token")
      requestBody = UrlForm(
        "client_id"     -> config.clientId,
        "grant_type"    -> "refresh_token",
        "refresh_token" -> refreshToken,
        "scope"         -> "offline_access",
      )
      postRequest = POST(requestBody, config.apiAuth / "oauth2" / "token")
      authResponse <- httpClient.expect[AuthResponse](postRequest)
      expiry = OffsetDateTime.now().plus(authResponse.expires_in.toLong, ChronoUnit.SECONDS)
      newAuthToken = TadoAuthToken(authResponse.access_token, expiry)
      _ <- logger.debug(s"Auth token refreshed $newAuthToken")
    yield (newAuthToken, authResponse.refresh_token)

  private def buildHttpClient(authToken: TadoAuthToken): Client[F] =
    val retryPolicy =
      RetryPolicy[F](
        backoff = RetryPolicy.exponentialBackoff(config.maxRetryTime, config.maxRetries),
      )

    Retry(retryPolicy):
      BearerTokenAuthClient[F](authToken.bearerToken):
        httpClient

  private def isTokenExpired(authToken: TadoAuthToken): Boolean =
    val now = OffsetDateTime.now()
    authToken.expiry.isBefore(now) || authToken.expiry.isEqual(now)

  private def atomicallyModifyAuthenticatedClient(f: Option[Client[F]] => F[Client[F]]): F[Client[F]] =
    atomicState.evalModify: state =>
      f(state.authenticatedClient).map: newAuthenticatedClient =>
        (state.copy(authenticatedClient = Some(newAuthenticatedClient)), newAuthenticatedClient)

  private def clearAuthenticatedClient(): F[Unit] =
    atomicState.update:
      _.copy(authenticatedClient = None)

  private def getAuthToken(): F[Option[TadoAuthToken]] =
    atomicState.get.map:
      _.authToken
