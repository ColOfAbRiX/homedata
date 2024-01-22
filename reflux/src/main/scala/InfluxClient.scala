package reflux

import reflux.api.*
import cats.{ Applicative, Functor }
import cats.effect.{ Async, Sync }
import cats.syntax.functor.*
import fs2.{ text, Stream }
import org.http4s.*
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.Credentials.Token
import org.http4s.headers.{ Accept, Authorization }
import org.http4s.headers.*
import org.http4s.Method.*
import com.github.plokhotnyuk.jsoniter_scala.core.{ readFromString, writeToString }

final case class InfluxClientConfig(
  serverUrl: Uri,
  token: InfluxAuthToken,
  organizationId: OrganizationId,
)

class InfluxClient[F[_]: Async](httpClient: Client[F], config: InfluxClientConfig)
  extends Http4sClientDsl[F]:

  private lazy val authHttpClient =
    Authenticator[F](config.token, httpClient)

  private val bucketUri: Uri =
    (config.serverUrl / "api" / "v2" / "buckets")

  private val queryUri: Uri =
    (config.serverUrl / "api" / "v2" / "query")

  private val writeUri: Uri =
    (config.serverUrl / "api" / "v2" / "write")

  // def write(retentionPolicy: String, measurement: String, values: Measurement*): F[Unit] =
  //   write(measurement, fs2.Stream(values: _*), Some(retentionPolicy))

  // def write(measurement: String, values: Measurement*): F[Unit] =
  //   write(measurement, fs2.Stream(values: _*))

  // def write[A: ToMeasurement](values: Iterable[A]): F[Unit] =
  //   write(values, None)

  // def write[A: ToMeasurement](values: Iterable[A], retentionPolicy: Option[String]): F[Unit] =
  //   write(fs2.Stream.fromIterator(values.iterator, 1024), retentionPolicy)

  // def write[A: ToMeasurement](values: fs2.Stream[F, A]): F[Unit] =
  //   write(values, None)

  // def write[A](values: fs2.Stream[F, A], retentionPolicy: Option[String])(using mapper: ToMeasurement[A]): F[Unit] =
  //   write(mapper.measurementName, values.map(mapper.write), retentionPolicy)

  // def write(measurement: String, values: fs2.Stream[F, Measurement]): F[Unit] =
  //   write(measurement, values, None)

  // private def nameValues(vs: Seq[(String, String)]) =
  //   vs.map(t => t._1 + "=" + t._2)

  // def write(measurement: String, values: fs2.Stream[F, Measurement], retentionPolicy: Option[String]): F[Unit] = {
  //   def toStr(m: Measurement) =
  //     s"${(measurement +: nameValues(m.tags)).mkString(",")} ${nameValues(m.values)
  //         .mkString(",")} ${m.time.map(_.toEpochMilli).getOrElse("")}\n"

  //   val requestUrl = writeUri.withOptionQueryParam("rp", retentionPolicy)
  //   val body       = values.map(toStr).through(text.utf8.encode)
  //   val request    = Request[F](method = POST, uri = requestUrl, body = body)

  //   httpClient
  //     .run(request)
  //     .use { response =>
  //       if (response.status.isSuccess)
  //         Applicative[F].unit
  //       else
  //         handleError(response)
  //           .as(())
  //           .compile
  //           .lastOrError
  //     }
  // }

  // def stream[A](query: String)(using reader: Read[A]): Stream[F, A] =
  //   stream[A](query, 3)

  // protected def stream[A](query: String, csvDataIndex: Int = 3)(using reader: Read[A]): Stream[F, A] =
  //   streamRaw(query, csvDataIndex).map(reader.read)

  // def streamRaw(query: String, csvDataIndex: Int = 3): Stream[F, CsvRow] =
  //   val requestUrl = queryUri.withQueryParam("chunked", "true")
  //   val body       = UrlForm("q" -> query)
  //   val headers    = Accept(MediaType.text.csv)
  //   val request    = POST(body, requestUrl, headers)

  //   httpClient
  //     .stream(request)
  //     .flatMap { response =>
  //       if (response.status.isSuccess)
  //         response.body.through(Csv.rows(csvDataIndex))
  //       else
  //         handleError(response)
  //     }

  // def asVector[A: Read](query: String): F[Vector[A]] =
  //   stream[A](query)
  //     .compile
  //     .toVector

  // def exec(query: String): F[Vector[String]] =
  //   stream[String](query, 2)
  //     .compile
  //     .toVector

  // def execBucket(query: String): F[Vector[String]] =
  //   bucket[String](query, 2)
  //     .compile
  //     .toVector

  private def handleError[A](response: Response[F]): Stream[F, A] =
    response.body
      .through(text.utf8.decode)
      .take(4096)
      .flatMap { s =>
        Stream.raiseError(InfluxException(response.status, s))
      }

  def rawInfluxRequest[R <: InfluxResponse: Manifest](influxRequest: InfluxRequest): F[R] =
    val body    = writeToString(influxRequest)
    val headers = Headers(`Content-Type`(MediaType.application.json))
    val request = POST(body, bucketUri, headers)

    println(request)
    println(body)
    authHttpClient
      .stream(request)
      .flatMap { response =>
        println(response)
        println(response.body.compile.toString())
        if (response.status.isSuccess)
          decodeAsResponse[R](response.body)
        else
          handleError(response)
      }
      .compile
      .lastOrError

  private def decodeAsResponse[R <: InfluxResponse: Manifest](body: EntityBody[F]): Stream[F, R] =
    body
      .through(text.utf8.decode)
      .through(text.lines)
      .filter(_.nonEmpty)
      .map { stringBody =>
        // readFromString[R]()
        ???
      }

  //  Builders  //

  def updateConfig(f: InfluxClientConfig => InfluxClientConfig): InfluxClient[F] =
    new InfluxClient(httpClient, f(config))

  //  Utils  //

  def createDatabase(name: String): F[Unit] =
    val request =
      InfluxRequest.CreateBucketRequest(
        description = None,
        name = name,
        orgID = config.organizationId.value,
        retentionRules = List.empty,
        rp = None,
        schemaType = None,
      )

    rawInfluxRequest[InfluxResponse.CreateBucketResponse](request)
      .void

object Authenticator:
  import org.typelevel.ci.*

  def apply[F[_]: Sync](token: InfluxAuthToken, httpClient: Client[F]): Client[F] =
    Client { request =>
      val authorization = Header.Raw(ci"Authorization", s"Token ${token.value}")
      val authHeaders   = request.headers.put(authorization)
      val authRequest   = request.withHeaders(authHeaders)
      httpClient.run(authRequest)
    }
