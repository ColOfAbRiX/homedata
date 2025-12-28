// package com.colofabrix.scala.homedata.influx

// import cats.effect.IO
// import com.colofabrix.scala.homedata.utils.*
// import com.colofabrix.scala.homedata.utils.pipes.*
// import com.colofabrix.scala.timeflux.*
// import com.colofabrix.scala.timeflux.measures.Measure
// import com.colofabrix.scala.timeflux.model.OrgId
// import org.typelevel.log4cats.Logger
// import org.typelevel.log4cats.slf4j.Slf4jLogger

// class InfluxReader(timefluxClient: TimefluxClient[IO], orgId: String) extends TimefluxDSL {

//   implicit private val logger: Logger[IO] =
//     Slf4jLogger.getLogger[IO]

//   def query(query: String): IO[fs2.Stream[IO, ResultRow]] =
//     for
//       _      <- fs2.Stream.info("Wiring readings into database...")
//       result <- timefluxClient.query(QueryRequest(query, Some(orgId)))
//     yield result

// }

// object InfluxReader extends TimefluxDSL {

//   implicit private val logger: Logger[IO] =
//     Slf4jLogger.getLogger[IO]

//   private lazy val orgName =
//     InfluxConfig.config.orgName.value

//   def apply(): IO[InfluxReader] =
//     for
//       _              <- logger.info("Initializing Influx reader...")
//       _              <- logger.debug(s"Influx configuration: ${InfluxConfig.config}")
//       timefluxClient <- TimefluxClient[IO](InfluxConfig.clientConfig)
//       orgId          <- initOrg(timefluxClient).map(_.value)
//       _              <- logger.debug(s"Ensuring bucket '${InfluxConfig.config.projectBucket}' exists...")
//       _              <- timefluxClient.createBucketIfMissing(InfluxConfig.config.projectBucket, orgId)
//       result          = new InfluxReader(timefluxClient, orgId)
//       _              <- logger.info(s"Initialized Influx reader on bucket ${InfluxConfig.config.projectBucket}")
//     yield result

//   private def initOrg(timefluxClient: TimefluxClient[IO]): IO[OrgId] =
//     logger.debug(s"Ensuring organization '$orgName' exists...") >>
//     timefluxClient
//       .createOrgIfMissing(orgName)
//       .flatMap {
//         case Some(org) =>
//           logger.info(s"Created organization '$orgName' with ID ${org.id}") >>
//           IO.pure(OrgId(org.id))
//         case None =>
//           timefluxClient.resolveOrgId(InfluxConfig.config.orgName)
//       }

// }
