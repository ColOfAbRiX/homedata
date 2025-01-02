package com.colofabrix.scala.cuttlefish

import cats.effect.Async
import cats.effect.std.AtomicCell
import cats.implicits.given
import com.colofabrix.scala.cuttlefish.api.*
import com.colofabrix.scala.cuttlefish.CuttlefishClient.*
import fs2.io.net.Network
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import org.http4s.*
import org.http4s.circe.CirceEntityDecoder.*
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.client.middleware.Logger
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.Method.*
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import scala.concurrent.duration.*

/**
 * Cuttlefish Client for Scala
 */
final class CuttlefishClient[F[_]: Async] private (
  httpClient: Client[F],
  config: CuttlefishConfig,
  atomicState: AtomicCell[F, CuttlefishClientState[F]],
) extends Http4sClientDsl[F]:

  implicit private val logger: SelfAwareStructuredLogger[F] =
    Slf4jLogger.getLogger[F]

  /**
   * Logs into the Octopus service
   */
  def login(username: String, password: String): F[Unit] =
    logger.debug(s"Login") >>
    setCredentials(username, password) >>
    loginRequest()

  /**
   * Logs out the Octopus service
   */
  def logout(): F[Unit] =
    logger.debug(s"Logout") >>
    clearCredentials() >>
    clearAuthToken() >>
    clearAuthenticatedClient()

  /**
   * Information about the Octopus account
   */
  def getAccountInfo(): F[AccountResponse] =
    for
      _      <- logger.debug(s"Get Account Info")
      client <- withAuthClient()
      request = GET(config.apiBase / "me")
      result <- client.expect[AccountResponse](request)
    yield result

  //  Internal operations  //

  private def loginRequest(): F[Unit] =
    getCredentials().flatMap: credentials =>
      val requestBody =
        UrlForm(
          "client_id"     -> "Cuttlefish-web-app",
          "grant_type"    -> "password",
          "scope"         -> "home.user",
          "username"      -> credentials.username,
          "password"      -> credentials.password,
          "client_secret" -> config.clientSecret,
        )

      val postRequest = POST(requestBody, config.apiAuth / "oauth" / "token")

      httpClient
        .expect[AuthResponse](postRequest)
        .flatMap { authResponse =>
          val expiry    = OffsetDateTime.now().plus(authResponse.expires_in.toLong, ChronoUnit.SECONDS)
          val authToken = CuttlefishAuthToken(authResponse.access_token, expiry)
          setAuthToken(authToken)
        }
        .adaptError { error =>
          CuttlefishError("Error while logging in", Some(error))
        }

  private def refreshTokenRequest(): F[Unit] =
    val requestBody =
      UrlForm(
        "grant_type"    -> "refresh_token",
        "refresh_token" -> "def",
        "client_id"     -> "Cuttlefish-web-app",
        "scope"         -> "home.user",
        "client_secret" -> config.clientSecret,
      )

    val postRequest = POST(requestBody, config.apiAuth)

    httpClient
      .expect[AuthResponse](postRequest)
      .flatMap { authResponse =>
        val expiry    = OffsetDateTime.now().plus(authResponse.expires_in.toLong, ChronoUnit.SECONDS)
        val authToken = CuttlefishAuthToken(authResponse.access_token, expiry)
        setAuthToken(authToken)
      }
      .handleErrorWith { error =>
        logger.debug(error)("Refresh token failed with error") >>
        logger.warn("Unauthenticated, trying to login...") >>
        loginRequest()
      }
      .onError { error =>
        logger.error(error)("Error while logging in")
      }
      .adaptError { error =>
        CuttlefishError("Error while refreshing API token", Some(error))
      }

  private def withAuthClient[A](retries: Int = 1): F[Client[F]] =
    logger.trace("Getting Authenticated Client") >>
    getAuthToken().flatMap:
      case None if retries <= 0 =>
        Async[F].raiseError(CuttlefishError("Cuttlefish could not log in"))
      case None =>
        Async[F].raiseError(CuttlefishError("Cuttlefish is not logged in"))
      case Some(authToken) if isTokenExpired(authToken) =>
        logger.info("Cuttlefish token expired, getting a new one") >>
        refreshTokenRequest() >> withAuthClient(retries - 1)
      case Some(authToken) =>
        getAuthenticatedClient().flatMap:
          case Some(client) =>
            Async[F].pure(client)
          case None =>
            for
              _      <- setAuthenticatedClient(buildHttpClient(authToken))
              _      <- logger.debug("New Octopus authenticated client")
              result <- withAuthClient(retries - 1)
            yield result

  private def buildHttpClient(authToken: CuttlefishAuthToken): Client[F] =
    Logger.colored[F](logBody = true, logHeaders = true):
      CuttlefishAuthenticatedClient[F](authToken.bearerToken):
        httpClient

  private def isTokenExpired(authToken: CuttlefishAuthToken): Boolean =
    authToken.expiry.minus(5, ChronoUnit.SECONDS).isBefore(OffsetDateTime.now())

  //  State management  //

  private def getCredentials(): F[CuttlefishCredentials] =
    atomicState.get.flatMap: state =>
      state.credentials match
        case Some(credentials) => Async[F].pure(credentials)
        case None              => Async[F].raiseError(CuttlefishError("No Octopus credentials provided"))

  private def getAuthenticatedClient(): F[Option[Client[F]]] =
    atomicState.get.map:
      _.authenticatedClient

  private def setAuthenticatedClient(client: Client[F]): F[Unit] =
    atomicState.update:
      _.copy(authenticatedClient = Some(client))

  private def clearAuthenticatedClient(): F[Unit] =
    atomicState.update:
      _.copy(authenticatedClient = None)

  private def setAuthToken(authToken: CuttlefishAuthToken): F[Unit] =
    atomicState.update:
      _.copy(authToken = Some(authToken))

  private def getAuthToken(): F[Option[CuttlefishAuthToken]] =
    atomicState.get.map:
      _.authToken

  private def clearAuthToken(): F[Unit] =
    atomicState.update:
      _.copy(authToken = None)

  private def setCredentials(username: String, password: String): F[Unit] =
    atomicState.update:
      _.copy(credentials = Some(CuttlefishCredentials(username = username, password = password)))

  private def clearCredentials(): F[Unit] =
    atomicState.update:
      _.copy(credentials = None)

/**
 * Cuttlefish Client for Scala
 */
object CuttlefishClient:

  final private case class CuttlefishClientState[F[_]](
    credentials: Option[CuttlefishCredentials] = None,
    authenticatedClient: Option[Client[F]] = None,
  )

  final private case class CuttlefishCredentials(
    username: String,
    password: String,
  )

  /** Creates a new instance of Cuttlefish client using the given client */
  def apply[F[_]: Async](maybeConfig: Option[CuttlefishConfig], httpClient: Client[F]): F[CuttlefishClient[F]] =
    for
      initialState <- AtomicCell[F].of(CuttlefishClientState[F](None, None, None))
      config        = maybeConfig.getOrElse(CuttlefishConfig.config)
      client        = new CuttlefishClient[F](httpClient, config, initialState)
    yield client

  /** Creates a new instance of Cuttlefish client using http4s Ember Client */
  def apply[F[_]: Async: Network](maybeConfig: Option[CuttlefishConfig]): F[CuttlefishClient[F]] =
    EmberClientBuilder
      .default[F]
      .withTimeout(30.seconds)
      .build
      .allocated
      .flatMap {
        case (httpClient, _) => CuttlefishClient(maybeConfig, httpClient)
      }
