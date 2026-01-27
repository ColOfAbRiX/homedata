package com.colofabrix.scala.timeflux

import cats.effect.Async
import cats.effect.std.AtomicCell
import cats.implicits.given
import com.colofabrix.scala.http4s.middleware.betterlogger.ClientLogger
import com.colofabrix.scala.timeflux.api.*
import com.colofabrix.scala.timeflux.config.*
import com.colofabrix.scala.timeflux.handlers.buckets.*
import com.colofabrix.scala.timeflux.handlers.orgs.*
import com.colofabrix.scala.timeflux.handlers.query.*
import com.colofabrix.scala.timeflux.handlers.write.*
import com.colofabrix.scala.timeflux.measures.*
import com.colofabrix.scala.timeflux.model.*
import com.colofabrix.scala.timeflux.security.SSLValidationClient
import com.colofabrix.scala.timeflux.TimefluxClient.*
import fs2.io.net.Network
import fs2.io.net.tls.TLSContext
import org.http4s.*
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.ember.client.EmberClientBuilder

/**
 * InfluxDB Client for Scala
 *
 * Reference: https://docs.influxdata.com/influxdb/v2/api/
 */
final class TimefluxClient[F[_]: Async] private (
  httpClient: Client[F],
  config: TimefluxConfig,
  atomicState: AtomicCell[F, TimefluxClientState[F]],
) extends Http4sClientDsl[F] {

  private val authenticator: TimefluxAuthentication[F] =
    new TimefluxAuthentication(httpClient, config, atomicState)

  /**
   * List organizations
   */
  def listOrgs(request: ListOrgsRequest): F[ListOrgsResponse] =
    prepareCall { (client, baseApiUrl) =>
      OrgRequestsHandler(client, baseApiUrl).listOrgs(request)
    }

  /**
   * Creates an organization
   */
  def createOrg(request: CreateOrgRequest): F[CreateOrgResponse] =
    prepareCall { (client, baseApiUrl) =>
      OrgRequestsHandler(client, baseApiUrl).createOrg(request)
    }

  /**
   * Checks if an organization exists and, if it doesn't, it creates it
   */
  def createOrgIfMissing(request: CreateOrgRequest): F[Option[CreateOrgResponse]] =
    prepareCall { (client, baseApiUrl) =>
      OrgRequestsHandler(client, baseApiUrl).createOrgIfMissing(request)
    }

  /**
   * Resolves an organization name to its ID (lookup only, errors if not found)
   */
  def resolveOrgId(orgName: OrgName): F[OrgId] =
    prepareCall { (client, baseApiUrl) =>
      val request = ListOrgsRequest(Some(orgName.value), None)
      OrgRequestsHandler(client, baseApiUrl)
        .listOrgs(request)
        .flatMap { resp =>
          resp.orgs.headOption match
            case Some(org) => OrgId(org.id).pure[F]
            case None      => TimefluxException(s"Organization '${orgName.value}' not found", None).raiseError
        }
    }

  /**
   * List one or all the buckets
   */
  def listBuckets(request: ListBucketRequest): F[ListBucketsResponse] =
    prepareCall { (client, baseApiUrl) =>
      BucketRequestsHandler(client, baseApiUrl).listBuckets(request)
    }

  /**
   * Creates a bucket
   */
  def createBucket(request: CreateBucketRequest): F[CreateBucketResponse] =
    prepareCall { (client, baseApiUrl) =>
      BucketRequestsHandler(client, baseApiUrl).createBucket(request)
    }

  /**
   * Deletes a bucket
   */
  def deleteBucket(request: DeleteBucketRequest): F[Unit] =
    prepareCall { (client, baseApiUrl) =>
      BucketRequestsHandler(client, baseApiUrl).deleteBucket(request)
    }

  /**
   * Checks if a bucket exists and, if it doesn't, it creates it
   */
  def createBucketIfMissing(request: CreateBucketRequest): F[Option[CreateBucketResponse]] =
    prepareCall { (client, baseApiUrl) =>
      BucketRequestsHandler(client, baseApiUrl).createBucketIfMissing(request)
    }

  /**
   * Writes a stream of TimefluxSerializable values in a bucket
   */
  def writeData[A: TimefluxSerializable](request: WriteRequest, values: fs2.Stream[F, A]): F[Unit] =
    prepareCall { (client, baseApiUrl) =>
      WriteRequestHandler(client, config, baseApiUrl)
        .writeRequest(request, values.through(TimefluxSerializable.toApiMeasureStream))
    }

  /**
   * Writes a stream of TimefluxSerializable values in a bucket
   */
  def writeMeasures(request: WriteRequest, values: fs2.Stream[F, Measure]): F[Unit] =
    prepareCall { (client, baseApiUrl) =>
      WriteRequestHandler(client, config, baseApiUrl).writeRequest(request, values)
    }

  /**
   * Queries InfluxDB and returns a stream of results
   */
  def query(request: QueryRequest): F[fs2.Stream[F, ResultRow]] =
    prepareCall { (client, baseApiUrl) =>
      QueryRequestHandler(client, baseApiUrl).queryRequest(request)
    }

  //  Internal operations  //

  private def prepareCall[A, B](f: (Client[F], Uri) => F[B]): F[B] =
    for
      baseApiUrl <- getApiUrl()
      client     <- authenticator.withAuthClient()
      result     <- f(client, baseApiUrl)
    yield result

  //  State management  //

  private def getApiUrl(): F[Uri] =
    atomicState.get.flatMap:
      _.serverUrl match {
        case None =>
          TimefluxException("No InfluxDB URL set.", None).raiseError
        case Some(serverUrl) =>
          serverUrl.addPath(config.apiBase).pure[F]
      }

}

/**
 * InfluxDB Client for Scala
 */
object TimefluxClient {

  final private[timeflux] case class TimefluxClientState[F[_]](
    credentials: Option[TimefluxCredentials] = None,
    authenticatedClient: Option[Client[F]] = None,
    serverUrl: Option[Uri] = None,
  )

  final private[timeflux] case class TimefluxCredentials(
    token: AuthToken,
  )

  /**
   * Creates a new instance of TimefluxClient client using http4s Ember Client
   */
  def apply[F[_]: Async: Network](
    clientConfig: TimefluxClientConfig,
    maybeConfig: Option[TimefluxConfig] = None,
  ): F[TimefluxClient[F]] =
    for
      config       <- maybeConfig.getOrElse(TimefluxConfig.config).pure[F]
      tlsContext   <- buildTlsContext(config)
      httpClient   <- buildHttpClient(config, tlsContext)
      initialState <- AtomicCell[F].of(buildInitialState[F](clientConfig))
      client        = new TimefluxClient[F](httpClient, config, initialState)
    yield client

  private def buildHttpClient[F[_]: Async: Network](config: TimefluxConfig, tlsContext: TLSContext[F]): F[Client[F]] =
    EmberClientBuilder
      .default[F]
      .withTimeout(config.httpTimeout)
      .withTLSContext(tlsContext)
      .build
      .allocated
      .map {
        case (httpClient, _) => ClientLogger(SSLValidationClient(config.ignoreSsl)(httpClient))
      }

  private def buildTlsContext[F[_]: Network](config: TimefluxConfig): F[TLSContext[F]] =
    if config.ignoreSsl then Network[F].tlsContext.insecure
    else Network[F].tlsContext.system

  private def buildInitialState[F[_]](clientConfig: TimefluxClientConfig): TimefluxClientState[F] =
    TimefluxClientState[F](
      serverUrl = Some(clientConfig.serverUrl),
      credentials = Some(TimefluxCredentials(clientConfig.authToken)),
    )

}
