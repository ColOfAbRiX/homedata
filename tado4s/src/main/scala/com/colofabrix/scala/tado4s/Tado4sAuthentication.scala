package com.colofabrix.scala.tado4s

import cats.effect.Async
import cats.effect.Deferred
import cats.effect.std.AtomicCell
import cats.implicits.given
import com.colofabrix.scala.tado4s.api.*
import com.colofabrix.scala.tado4s.security.*
import com.colofabrix.scala.tado4s.store.{ Tado4sTokenStore, TadoRefreshToken }
import com.colofabrix.scala.tado4s.Tado4sAuthentication.*
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

/**
 * Tado Authenticator - Thread-safe authentication state machine
 *
 * Ensures:
 *   - No thundering herd on token refresh
 *   - Single-flight pattern: only one fiber refreshes, others wait
 *   - File access only by leader fiber
 *
 * See: https://support.tado.com/en/articles/8565472-how-do-i-authenticate-to-access-the-rest-api
 */
final class Tado4sAuthentication[F[_]: Async] private (
  httpClient: Client[F],
  config: TadoConfig,
  atomicState: AtomicCell[F, AuthState[F]],
) extends Http4sClientDsl[F] {

  private val logger: SelfAwareStructuredLogger[F] =
    Slf4jLogger.getLogger[F]

  /**
   * Authenticate with a refresh token.
   */
  def authenticate(initialRefreshToken: TadoRefreshToken): F[Unit] =
    Deferred[F, Either[Throwable, AuthenticatedData[F]]].flatMap { gate =>
      atomicState.modify {
        case AuthState.Unauthenticated() =>
          (AuthState.Authenticating(gate), doInitialAuth(gate, initialRefreshToken))

        case AuthState.Authenticating(existingGate) =>
          (AuthState.Authenticating(existingGate), waitOnGate(existingGate).void)

        case state @ AuthState.Authenticated(_) =>
          (state, logger.debug("Already authenticated, skipping"))

        case state @ AuthState.Refreshing(_, existingGate) =>
          (state, waitOnGate(existingGate).void)
      }.flatten
    }

  /**
   * Logout and clear all tokens including the persisted token file. Transitions to Unauthenticated state.
   */
  def logout(): F[Unit] =
    for
      _ <- logger.debug("Logout")
      _ <- Tado4sTokenStore.clear[F]()
      _ <- atomicState.set(AuthState.Unauthenticated())
      _ <- logger.info("Logged out, token file deleted")
    yield ()

  /**
   * Get an authenticated HTTP client.
   */
  def getAuthenticatedClient(): F[Client[F]] =
    atomicState.get.flatMap {
      case AuthState.Authenticated(data) if !data.authToken.isExpired =>
        logger.trace("Returning cached authenticated client") >>
        data.client.pure[F]

      case AuthState.Authenticated(data) =>
        logger.debug("Token expired, initiating refresh") >>
        transitionToRefreshing(data)

      case AuthState.Authenticating(gate) =>
        logger.debug("Authentication in progress, waiting...") >>
        waitOnGate(gate).map(_.client)

      case AuthState.Refreshing(_, gate) =>
        logger.debug("Refresh in progress, waiting...") >>
        waitOnGate(gate).map(_.client)

      case AuthState.Unauthenticated() =>
        Tado4sError("Not authenticated. Call authenticate() first.").raiseError
    }

  // Wait on a gate and rethrow any errors
  private def waitOnGate(gate: Gate[F]): F[AuthenticatedData[F]] =
    gate.get.flatMap {
      case Right(data) => data.pure[F]
      case Left(error) => error.raiseError
    }

  // Transition from Authenticated to Refreshing state. Uses atomic modify to ensure only one fiber becomes the leader.
  private def transitionToRefreshing(currentData: AuthenticatedData[F]): F[Client[F]] =
    Deferred[F, Either[Throwable, AuthenticatedData[F]]].flatMap { gate =>
      atomicState.modify {
        case AuthState.Authenticated(data) if data.authToken.isExpired =>
          (AuthState.Refreshing(data, gate), doRefreshAsLeader(gate, data))

        case AuthState.Refreshing(_, existingGate) =>
          (AuthState.Refreshing(currentData, existingGate), waitOnGate(existingGate).map(_.client))

        case state @ AuthState.Authenticated(data) =>
          (state, data.client.pure[F])

        case state =>
          (state, Tado4sError(s"Invalid state for refresh: $state").raiseError)
      }.flatten
    }

  /* Leader performs initial authentication.
   *   - Reads file to check for stored token (exclusive - we're in Authenticating state)
   *   - Gets auth token from Tado API
   *   - Writes file with new token
   *   - Signals all waiters via gate
   */
  private def doInitialAuth(gate: Gate[F], initialToken: TadoRefreshToken): F[Unit] =
    val work =
      for
        _                     <- logger.info("Starting initial authentication...")
        storedToken           <- Tado4sTokenStore.load[F]()
        tokenToUse             = selectNewerToken(storedToken, Some(initialToken)).getOrElse(initialToken)
        (authToken, newToken) <- refreshRequest(tokenToUse)
        client                 = buildHttpClient(authToken)
        data                   = AuthenticatedData(newToken, authToken, client)
        _                     <- Tado4sTokenStore.save(newToken)
        _                     <- atomicState.set(AuthState.Authenticated(data))
        _                     <- gate.complete(Right(data))
        _                     <- logger.info("Authentication successful")
      yield ()

    work.handleErrorWith { error =>
      logger.error(error)("Authentication failed") >>
      atomicState.set(AuthState.Unauthenticated()) >>
      gate.complete(Left(error)) >>
      error.raiseError
    }

  /* Leader performs token refresh.
   *   - Makes API call to get new tokens
   *   - Writes file with new token (exclusive access)
   *   - Signals all waiters via gate
   *   - On error, restores previous state
   */
  private def doRefreshAsLeader(gate: Gate[F], currentData: AuthenticatedData[F]): F[Client[F]] =
    val work =
      for
        _                        <- logger.info("Refreshing authentication token...")
        (newAuthToken, newToken) <- refreshRequest(currentData.refreshToken)
        newClient                 = buildHttpClient(newAuthToken)
        newData                   = AuthenticatedData(newToken, newAuthToken, newClient)
        _                        <- Tado4sTokenStore.save(newToken)
        _                        <- atomicState.set(AuthState.Authenticated(newData))
        _                        <- gate.complete(Right(newData))
        _                        <- logger.info("Token refresh successful")
      yield newClient

    work.handleErrorWith { error =>
      logger.error(error)("Token refresh failed, restoring previous state") >>
      // Restore previous state on failure
      atomicState.set(AuthState.Authenticated(currentData)) >>
      gate.complete(Left(error)) >>
      error.raiseError
    }

  // Select the newer of two tokens based on issueTime
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
      authResponse   <- httpClient.expect[AuthResponse](postRequest)
      now             = OffsetDateTime.now()
      expiry          = now.plus(authResponse.expires_in.toLong, ChronoUnit.SECONDS)
      issueTime       = now.minusSeconds(5)
      newAuthToken    = TadoAuthToken(authResponse.access_token, expiry)
      newRefreshToken = TadoRefreshToken(authResponse.refresh_token, issueTime)
    yield (newAuthToken, newRefreshToken)

  private def buildHttpClient(authToken: TadoAuthToken): Client[F] =
    val retryPolicy =
      RetryPolicy[F](
        backoff = RetryPolicy.exponentialBackoff(config.maxRetryTime, config.maxRetries),
      )

    Retry(retryPolicy):
      BearerTokenAuthClient[F](authToken.bearerToken):
        httpClient

}

object Tado4sAuthentication {

  private type Gate[F[_]] = Deferred[F, Either[Throwable, AuthenticatedData[F]]]

  /**
   * Create a new Tado4sAuthentication instance with its own internal state. The authentication state is completely
   * encapsulated - not visible to callers.
   */
  def apply[F[_]: Async](httpClient: Client[F], config: TadoConfig): F[Tado4sAuthentication[F]] =
    for
      atomicState <- AtomicCell[F].of[AuthState[F]](AuthState.Unauthenticated())
      result       = new Tado4sAuthentication[F](httpClient, config, atomicState)
    yield result

  // Authentication state machine - ensures thread-safe state transitions.
  private enum AuthState[F[_]] {

    case Unauthenticated()
    case Authenticating(gate: Gate[F])
    case Authenticated(data: AuthenticatedData[F])
    case Refreshing(current: AuthenticatedData[F], gate: Gate[F])

  }

  // Authenticated data - contains all tokens and the configured client. Private to Tado4sAuthentication.
  final private case class AuthenticatedData[F[_]](
    refreshToken: TadoRefreshToken,
    authToken: TadoAuthToken,
    client: Client[F],
  )

  // Auth token with expiry tracking. Private to Tado4sAuthentication.
  final private case class TadoAuthToken(
    bearerToken: String,
    expiry: OffsetDateTime,
  ):
    def isExpired: Boolean =
      val now = OffsetDateTime.now()
      expiry.isBefore(now) || expiry.isEqual(now)

    override def toString(): String =
      s"TadoAuthToken(" +
      s"bearerToken=${bearerToken.take(8)}...${bearerToken.takeRight(8)}, " +
      s"expiry=${expiry.truncatedTo(ChronoUnit.SECONDS).toLocalDateTime}" +
      s")"

}
