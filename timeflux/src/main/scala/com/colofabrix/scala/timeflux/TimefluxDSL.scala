package com.colofabrix.scala.timeflux

import com.colofabrix.scala.timeflux.api.*
import com.colofabrix.scala.timeflux.measures.*

trait TimefluxDSL {

  extension [F[_]](timefluxClient: TimefluxClient[F]) {

    // --- Organizations API ---

    /**
     * List organizations, optionally filtered by name
     */
    def listOrgs(name: Option[String] = None): F[ListOrgsResponse] =
      timefluxClient.listOrgs(ListOrgsRequest(name, None))

    /**
     * Creates an organization
     */
    def createOrg(
      name: String,
      description: Option[String] = None,
    ): F[CreateOrgResponse] =
      timefluxClient.createOrg(CreateOrgRequest(name, description))

    /**
     * Checks if an organization exists and, if it doesn't, it creates it
     */
    def createOrgIfMissing(
      name: String,
      description: Option[String] = None,
    ): F[Option[CreateOrgResponse]] =
      timefluxClient.createOrgIfMissing(CreateOrgRequest(name, description))

    // --- Buckets API ---

    /**
     * List one or all the buckets
     */
    def listBuckets(
      name: Option[String] = None,
      orgID: Option[String] = None,
    ): F[ListBucketsResponse] =
      timefluxClient.listBuckets(ListBucketRequest(name, orgID))

    /**
     * Creates a bucket
     */
    def createBucket(
      name: String,
      orgID: String,
      description: Option[String] = None,
      retentionRules: List[RetentionRules] = List.empty,
    ): F[CreateBucketResponse] =
      timefluxClient.createBucket(
        CreateBucketRequest(name, orgID, description, retentionRules),
      )

    /**
     * Checks if a bucket exists and, if it doesn't, it creates it
     */
    def createBucketIfMissing(
      name: String,
      orgID: String,
      description: Option[String] = None,
      retentionRules: List[RetentionRules] = List.empty,
    ): F[Option[CreateBucketResponse]] =
      timefluxClient.createBucketIfMissing(
        CreateBucketRequest(name, orgID, description, retentionRules),
      )

    // --- Write API ---

    /**
     * Writes a stream of TimefluxSerializable values in a bucket
     */
    def writeData[A: TimefluxSerializable](
      bucket: String,
      orgID: String,
      values: fs2.Stream[F, A],
      precision: Option[TimePrecision] = None,
      batchWrites: Option[Int] = None,
    ): F[Unit] =
      val fullPrecision = precision.getOrElse(TimePrecision.Milliseconds)
      val request       = WriteRequest(bucket, orgID, fullPrecision, batchWrites)
      timefluxClient.writeData(request, values)

    /**
     * Writes a stream of TimefluxSerializable values in a bucket
     */
    def writeMeasures(
      bucket: String,
      orgID: String,
      values: fs2.Stream[F, Measure],
      precision: Option[TimePrecision] = None,
      batchWrites: Option[Int] = None,
    ): F[Unit] =
      val fullPrecision = precision.getOrElse(TimePrecision.Milliseconds)
      val request       = WriteRequest(bucket, orgID, fullPrecision, batchWrites)
      timefluxClient.writeMeasures(request, values)

  }

}
