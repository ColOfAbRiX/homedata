// package com.colofabrix.scala.homedata.octopus

// import cats.effect.IO
// import dev.kovstas.fs2throttler.Throttler
// import fs2.{ Chunk, Stream }
// import java.time.OffsetDateTime
// import org.typelevel.log4cats.Logger
// import org.typelevel.log4cats.slf4j.Slf4jLogger
// import scala.concurrent.duration.*

// object Octopus:

//   private type PagePull[A] =
//     Int => IO[(Chunk[A], Boolean)]

//   implicit private val logger: Logger[IO] =
//     Slf4jLogger.getLogger[IO]

//   def pullReadings(from: OffsetDateTime, to: OffsetDateTime): Stream[IO, OctopusReading] =
//     val electricityReadings = pullPaged(OctopusElectricity.pull(Some(from), Some(to), _, OctopusConfig.PageSize))
//     val gasReadings         = pullPaged(OctopusGas.pull(Some(from), Some(to), _, OctopusConfig.PageSize))

//     (electricityReadings merge gasReadings)

//   private def pullPaged[A](pull: PagePull[A]): Stream[IO, A] =
//     Stream
//       .unfoldLoopEval(1) { pageNumber =>
//         pull(pageNumber).map {
//           case (readings, true)  => (readings, Some(pageNumber + 1))
//           case (readings, false) => (readings, None)
//         }
//       }
//       .through(Throttler.throttle(1, 1.second, Throttler.Shaping))
//       .flatMap(Stream.chunk)
//       .evalTap(reading => logger.debug(s"Octopus reading: $reading"))
