package com.colofabrix.scala.timeflux.handlers.query

import cats.effect.Async
import cats.effect.Ref
import cats.implicits.given
import com.colofabrix.scala.timeflux.api.*
import com.colofabrix.scala.timeflux.handlers.ErrorHandling.*
import com.colofabrix.scala.timeflux.model.ResultRow
import fs2.data.csv.*
import io.github.arainko.ducktape.*
import org.http4s.*
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.Method.*
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import scala.collection.immutable.ListMap

final private[timeflux] class QueryRequestHandler[F[_]: Async](httpClient: Client[F], baseApiUrl: Uri)
  extends Http4sClientDsl[F] {

  implicit private val logger: SelfAwareStructuredLogger[F] =
    Slf4jLogger.getLogger[F]

  def queryRequest(request: QueryRequest): F[fs2.Stream[F, ResultRow]] =
    val headers =
      Headers(
        "Content-Type" -> "application/vnd.flux",
        "Accept"       -> "application/csv",
      )

    val requestParams =
      request
        .to[QueryRequest.QueryGetRequest]
        .toQueryParams

    val http4sRequest =
      POST(
        uri = (baseApiUrl / "query").withQueryParams(requestParams),
        headers = headers,
        body = request.query,
      )

    logger.debug(s"Called queryRequest() with $request") >>
    httpClient
      .run(http4sRequest)
      .use { response =>
        handleClientRunError(response) >>
        processSuccess(response)
      }

  private def processSuccess(response: Response[F]): F[fs2.Stream[F, ResultRow]] =
    Ref.of[F, Option[Vector[String]]](None).map { headersRef =>
      response
        .body
        .through(fs2.text.utf8.decode)
        .through(lowlevel.rows[F, String]())
        .evalMapFilter { row =>
          headersRef.modify {
            case None =>
              val cols = row.values.toList.toVector
              (Some(cols), None)
            case Some(cols) =>
              val rowData =
                ListMap.from(
                  cols
                    .zip(row.values.toList)
                    .filter { case (key, _) => key.nonEmpty },
                )
              (Some(cols), Some(ResultRow(rowData)))
          }
        }
    }

}
