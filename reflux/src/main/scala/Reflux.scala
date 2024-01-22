package reflux

import cats.effect.{ Async, IO }
import org.http4s.Uri
import org.http4s.Uri.{ Authority, Path }
import org.http4s.client.Client
import org.http4s.ember.client.EmberClientBuilder
import reflux.api.*

object Reflux:

  /**
   * THe url parameter can contain InfluxDB credentials and the database name as following:
   * htpp[s]://user:password@host[:port]/[database]
   */
  def client[F[_]: Async](
    http: Client[F],
    serverUrl: Uri,
    orgId: OrganizationId,
    token: InfluxAuthToken,
  ): InfluxClient[F] =
    val config =
      InfluxClientConfig(
        serverUrl = serverUrl.withPath(Path.empty),
        token = token,
        organizationId = orgId,
      )

    new InfluxClient[F](http, config)

  def clientIO(serverUrl: Uri, orgId: OrganizationId, token: InfluxAuthToken): InfluxClient[IO] =
    val httpClient =
      EmberClientBuilder
        .default[IO]
        .build
        .allocated
        .unsafeRunSync()(cats.effect.unsafe.IORuntime.global)
        ._1

    client(httpClient, serverUrl, orgId, token)
