package com.colofabrix.scala.timeflux

import cats.effect.Async
import com.colofabrix.scala.timeflux.api.*
import com.colofabrix.scala.timeflux.measures.*

trait TimefluxDSL:

  extension [F[_]: Async](timefluxClient: TimefluxClient[F])

    /**
     * List one or all the buckets
     */
    def listBuckets(name: Option[String] = None): F[ListBucketsResponse] =
      timefluxClient.listBuckets(
        ListBucketRequest(name, None),
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
    def writeData[A: TimefluxSerializable](
      bucket: String,
      values: fs2.Stream[F, A],
      precision: Option[TimePrecision] = None,
      batchWrites: Option[Int] = None,
    ): F[Unit] =
      val fullPrecision = precision.getOrElse(TimePrecision.Milliseconds)
      val request       = WriteRequest(bucket, None, fullPrecision, batchWrites)
      timefluxClient.writeData(request, values)

    /**
     * Writes a stream of TimefluxSerializable values in a bucket
     */
    def writeMeasures(
      bucket: String,
      values: fs2.Stream[F, Measure],
      precision: Option[TimePrecision] = None,
      batchWrites: Option[Int] = None,
    ): F[Unit] =
      val fullPrecision = precision.getOrElse(TimePrecision.Milliseconds)
      val request       = WriteRequest(bucket, None, fullPrecision, batchWrites)
      timefluxClient.writeMeasures(request, values)
