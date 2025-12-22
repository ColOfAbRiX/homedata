package com.colofabrix.scala.tado4s

import cats.effect.Async
import cats.effect.std.AtomicCell
import cats.implicits.given
import com.colofabrix.scala.tado4s.api.*
import com.colofabrix.scala.tado4s.security.*
import com.colofabrix.scala.tado4s.store.{Tado4sTokenStore, TadoRefreshToken}
import com.colofabrix.scala.tado4s.Tado4sClient.*
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import org.http4s.*
import org.http4s.circe.CirceEntityDecoder.*
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.client.middleware.{Retry, RetryPolicy}
import org.http4s.Method.*
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

/**
 * Tado Authenticator
 *
 * See: https://support.tado.com/en/articles/8565472-how-do-i-authenticate-to-access-the-rest-api
 */
final private[tado4s] class Tado4sAuthentication[F[_]: Async](
  httpClient: Client[F],
  config: TadoConfig,
  atomicState: AtomicCell[F, TadoClientState[F]],
) extends Http4sClientDsl[F]:

  private val logger: SelfAwareStructuredLogger[F] =
    Slf4jLogger.getLogger[F]

  /**
   * Authenticate with a refresh token. Saves the token to ~/.tado4s.conf for future use.
   */
  def authenticate(initialRefreshToken: TadoRefreshToken): F[Unit] =
    for
      _ <- logger.debug("Authenticating with refresh token")
      _ <- ensureLatestRefreshToken(Some(initialRefreshToken))
      _ <- logger.info("Authentication successful, token saved to ~/.tado4s.conf")
    yield ()

  /**
   * Authenticate with a refresh token. Saves the token to ~/.tado4s.conf for future use.
   */
  def authenticate(): F[Unit] =
    for
      _ <- logger.debug("Authenticating with refresh token")
      _ <- ensureLatestRefreshToken(None)
      _ <- logger.info("Authentication successful, token saved to ~/.tado4s.conf")
    yield ()

  /**
   * Logout and clear all tokens including the persisted token file.
   */
  def logout(): F[Unit] =
    for
      _ <- logger.debug("Logout")
      _ <- Tado4sTokenStore.clear[F]()
      _ <- setNewRefreshToken(None)
      _ <- logger.info("Logged out, token file deleted")
    yield ()

  /**
   * Get an authenticated HTTP client. Will attempt to load token from file if not in memory.
   */
  def getAuthenticatedClient[A](retries: Int = 1): F[Client[F]] =
    ensureLatestRefreshToken(None) >>
    getAuthToken().flatMap {
      case None =>
        logger.debug("No Authentication Token, attempting to get one via Refresh Token") >>
        handleTokenRefresh() >>
        getAuthenticatedClient(retries - 1)
      case Some(authToken) if isTokenExpired(authToken) =>
        logger.info("Tado token expired") >>
        logger.debug(s"Expired token: $authToken") >>
        clearAuthenticatedClient() >>
        handleTokenRefresh() >>
        getAuthenticatedClient(retries - 1)
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

  private def ensureLatestRefreshToken(initialRefreshToken: Option[TadoRefreshToken]): F[TadoRefreshToken] =
    atomicState.get.flatMap {
      case TadoClientState(Some(refreshToken), _, _) =>
        refreshToken.pure
      case TadoClientState(None, _, _) =>
        Tado4sTokenStore
          .load[F]()
          .map(selectNewerToken(_, initialRefreshToken))
          .flatMap {
            case Some(refreshToken) =>
              logger.debug(s"Using refresh token with issueTime=${refreshToken.issueTime}") >>
              Tado4sTokenStore.save[F](refreshToken) >>
              atomicState.update(_.copy(refreshToken = Some(refreshToken))) >>
              refreshToken.pure
            case None =>
              Tado4sError("No refresh token available. Call authenticate() with an initial token first.").raiseError
          }
    }

  private def selectNewerToken(
    storedToken: Option[TadoRefreshToken],
    initialToken: Option[TadoRefreshToken],
  ): Option[TadoRefreshToken] =
    (storedToken, initialToken) match
      case (Some(stored), Some(initial)) =>
        if stored.issueTime.isAfter(initial.issueTime) then Some(stored) else Some(initial)
      case (Some(stored), None) =>
        Some(stored)
      case (None, Some(initial)) =>
        Some(initial)
      case (None, None) =>
        None

  private def handleTokenRefresh(): F[Unit] =
    atomicState
      .evalUpdate {
        case state @ TadoClientState(Some(refreshToken), _, _) =>
          for
            (newAuth, newRefresh) <- refreshRequest(refreshToken)
            _                     <- Tado4sTokenStore.save[F](newRefresh)
            result                 = state.copy(authToken = Some(newAuth), refreshToken = Some(newRefresh))
          yield result
        case TadoClientState(None, _, _) =>
          Tado4sError("No refresh token provided. Please call authenticate() first.").raiseError
      }
      .handleErrorWith { error =>
        val tadoError = Tado4sError("Error while refreshing API token", Some(error))
        logger.error(tadoError)("Error while refreshing API token") >>
        tadoError.raiseError
      }

  private def refreshRequest(refreshToken: TadoRefreshToken): F[(TadoAuthToken, TadoRefreshToken)] =
    val requestBody =
      UrlForm(
        "client_id"     -> config.clientId,
        "grant_type"    -> "refresh_token",
        "refresh_token" -> refreshToken.token,
        "scope"         -> "offline_access",
      )

    val postRequest = POST(requestBody, config.apiAuth / "oauth2" / "token")

    for
      _              <- logger.info("Refreshing authentication token")
      authResponse   <- httpClient.expect[AuthResponse](postRequest)
      now             = OffsetDateTime.now()
      expiry          = now.plus(authResponse.expires_in.toLong, ChronoUnit.SECONDS)
      issueTime       = now.minusSeconds(5)
      newAuthToken    = TadoAuthToken(authResponse.access_token, expiry)
      newRefreshToken = TadoRefreshToken(authResponse.refresh_token, issueTime)
      _              <- logger.debug(s"Auth token refreshed, new token saved to file")
    yield (newAuthToken, newRefreshToken)

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

  private def setNewRefreshToken(refreshToken: Option[TadoRefreshToken]): F[Unit] =
    atomicState.update { state =>
      state.copy(refreshToken = refreshToken, authToken = None, authenticatedClient = None)
    }
