package com.colofabrix.scala.tado4s

import cats.effect.Async
import cats.effect.std.AtomicCell
import cats.implicits.given
import com.colofabrix.scala.tado4s.api.*
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

  def login(username: String, password: String): F[Unit] =
    logger.debug("Login") >>
    atomicState.update { state =>
      lazy val credentials = TadoCredentials(username, password)
      state.credentials match
        case None =>
          state.copy(credentials = Some(credentials))
        case Some(oldCredentials) if credentials != oldCredentials =>
          state.copy(credentials = Some(credentials), authToken = None, authenticatedClient = None)
        case Some(_) =>
          state
    } >> handleLogin()

  def logout(): F[Unit] =
    logger.debug("Logout") >>
    atomicState.update:
      _.copy(credentials = None, authToken = None, authenticatedClient = None)

  def withAuthClient[A](retries: Int = 1): F[Client[F]] =
    getAuthToken().flatMap {
      case None if retries <= 0 =>
        Tado4sError("Tado4s could not log in").raiseError
      case None =>
        Tado4sError("Tado4s is not logged in").raiseError
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

  private def handleLogin(): F[Unit] =
    atomicState
      .evalUpdate {
        case TadoClientState(None, _, _) =>
          Tado4sError("No Tado credentials provided").raiseError
        case state @ TadoClientState(Some(credentials), _, _) =>
          loginRequest(credentials).map(authToken => state.copy(authToken = Some(authToken)))
      }
      .adaptError { error =>
        Tado4sError("Error while logging in", Some(error))
      }

  private def loginRequest(credentials: TadoCredentials): F[TadoAuthToken] =
    val requestBody =
      UrlForm(
        "client_id"     -> "tado-web-app",
        "client_secret" -> config.clientSecret,
        "grant_type"    -> "password",
        "password"      -> credentials.password,
        "scope"         -> "home.user",
        "username"      -> credentials.username,
      )

    val postRequest = POST(requestBody, config.apiAuth / "oauth" / "token")

    logger.info("Sending login request") >>
    httpClient
      .expect[AuthResponse](postRequest)
      .flatMap { authResponse =>
        val expiry    = OffsetDateTime.now().plus(authResponse.expires_in.toLong, ChronoUnit.SECONDS)
        val authToken = TadoAuthToken(authResponse.access_token, authResponse.refresh_token, expiry)
        logger.trace(s"Login authentication response: $authResponse}") >>
        logger.debug(s"Logged in with token $authToken") >>
        authToken.pure[F]
      }

  private def handleTokenRefresh(): F[Unit] =
    atomicState
      .evalUpdate {
        case TadoClientState(None, None, _) =>
          Tado4sError("No Tado credentials provided").raiseError
        case state @ TadoClientState(Some(credentials), None, _) =>
          logger.warn("Cannot refresh token because unauthenticated, trying to login instead...") >>
          loginRequest(credentials).map(authToken => state.copy(authToken = Some(authToken)))
        case state @ TadoClientState(_, Some(authToken), _) =>
          refreshRequest(authToken).map(authToken => state.copy(authToken = Some(authToken)))
      }
      .handleErrorWith { error =>
        logger.debug(error)("Refresh token failed with error") >>
        logger.warn("Cannot refresh token, trying to login...") >>
        clearAuthToken() >>
        handleLogin()
      }
      .handleErrorWith { error =>
        val tadoError = Tado4sError("Error while refreshing API token", Some(error))
        logger.error(tadoError)("Error while refreshing API token") >>
        tadoError.raiseError
      }

  private def refreshRequest(authToken: TadoAuthToken): F[TadoAuthToken] =
    val requestBody =
      UrlForm(
        "client_id"     -> "tado-web-app",
        "client_secret" -> config.clientSecret,
        "grant_type"    -> "refresh_token",
        "refresh_token" -> authToken.refreshToken,
        "scope"         -> "home.user",
      )

    val postRequest = POST(requestBody, config.apiAuth / "oauth" / "token")

    logger.info("Refreshing authentication token") >>
    httpClient
      .expect[AuthResponse](postRequest)
      .flatMap { authResponse =>
        val expiry       = OffsetDateTime.now().plus(authResponse.expires_in.toLong, ChronoUnit.SECONDS)
        val newAuthToken = TadoAuthToken(authResponse.access_token, authResponse.refresh_token, expiry)
        logger.trace(s"Refresh authentication response: $authResponse}") >>
        logger.debug(s"Auth token refreshed $newAuthToken") >>
        newAuthToken.pure[F]
      }

  private def buildHttpClient(authToken: TadoAuthToken): Client[F] =
    val retryPolicy =
      RetryPolicy[F](
        backoff = RetryPolicy.exponentialBackoff(config.maxRetryTime, config.maxRetries),
      )

    Retry(retryPolicy):
      TadoAuthenticatedClient[F](authToken.bearerToken):
        httpClient

  private def isTokenExpired(authToken: TadoAuthToken): Boolean =
    val now = OffsetDateTime.now()
    authToken.expiry.isBefore(now) || authToken.expiry.isEqual(now)

  //  State management  //

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

  private def clearAuthToken(): F[Unit] =
    atomicState.update:
      _.copy(authToken = None)
