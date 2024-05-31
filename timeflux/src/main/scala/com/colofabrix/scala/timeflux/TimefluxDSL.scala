package com.colofabrix.scala.timeflux

import cats.effect.Async
import cats.implicits.given
import com.colofabrix.scala.timeflux.api.*
import com.colofabrix.scala.timeflux.config.*
import com.colofabrix.scala.timeflux.measures.*
import com.colofabrix.scala.timeflux.model.*
import com.colofabrix.scala.timeflux.TimefluxClient.*
import fs2.io.net.Network
import io.odin.*
import io.odin.formatter.Formatter
import org.http4s.*
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.Method.*

trait TimefluxDSL:

  extension [F[_]: Async](timefluxClient: TimefluxClient[F])

    /**
     * List one or all the buckets
     */
    def listBuckets(name: Option[String] = None): F[ListBucketsResponse] =
      timefluxClient.listBuckets(
        ListBucketRequest(name),
      )

    /**
     * Creates a bucket
     */
    def createBucket(
      name: String,
      description: Option[String] = None,
      retentionRules: List[RetentionRules] = List.empty,
    ): F[CreateBucketResponse] =
      timefluxClient.createBucket(
        CreateBucketRequest(name, None, description, retentionRules),
      )

    /**
     * Checks if a bucket exists and, if it doesn't, it creates it
     */
    def createBucketIfMissing(
      name: String,
      description: Option[String] = None,
      retentionRules: List[RetentionRules] = List.empty,
    ): F[Option[CreateBucketResponse]] =
      timefluxClient.createBucketIfMissing(
        CreateBucketRequest(name, None, description, retentionRules),
      )

    /**
     * Writes a stream of TimefluxSerializable values in a bucket
     */
    def write[A: TimefluxSerializable](
      bucket: String,
      values: fs2.Stream[F, A],
      precision: Option[String] = None,
    ): F[Unit] =
      timefluxClient.write(
        WriteRequest(bucket, None, precision),
        values,
      )
