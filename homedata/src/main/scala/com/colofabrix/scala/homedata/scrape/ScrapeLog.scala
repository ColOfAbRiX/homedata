package com.colofabrix.scala.homedata.scrape

import cats.effect.*
import cats.effect.std.Queue
import cats.implicits.*
import java.nio.file.{ Files as JFiles, Path, StandardOpenOption }
import java.time.LocalDate
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import scala.concurrent.duration.*
import scala.jdk.CollectionConverters.*

enum ScrapeService {
  case Tado, Octopus
}

final case class ScrapeEntry(
  service: ScrapeService,
  date: LocalDate,
  entityId: String,
)

final class ScrapeLog[F[_]: Async] private (
  entries: Set[ScrapeEntry],
  writeQueue: Queue[F, ScrapeEntry],
  path: Path,
) {

  private val logger: Logger[F] = Slf4jLogger.getLogger[F]

  def contains(service: ScrapeService, date: LocalDate, entityId: String): Boolean =
    entries.contains(ScrapeEntry(service, date, entityId))

  def logEntry(service: ScrapeService, date: LocalDate, entityId: String): F[Unit] =
    writeQueue.offer(ScrapeEntry(service, date, entityId))

  def runWriter: fs2.Stream[F, Nothing] =
    fs2.Stream
      .fromQueueUnterminated(writeQueue)
      .groupWithin(100, 100.milliseconds)
      .evalMap(appendToFile)
      .drain

  private def appendToFile(batch: fs2.Chunk[ScrapeEntry]): F[Unit] =
    logger.debug(s"Writing ${batch.size} entries to scrape log") >>
    Sync[F].blocking {
      val lines = batch.toList.map(e => s"${e.service.toString.toLowerCase},${e.date},${e.entityId}\n")
      JFiles.write(path, lines.mkString.getBytes, StandardOpenOption.APPEND, StandardOpenOption.CREATE)
      ()
    }

}

object ScrapeLog {

  private val LinePattern =
    """^(\w+),(\d{4}-\d{2}-\d{2}),(.+)$""".r

  def apply[F[_]: Async](path: Path): F[ScrapeLog[F]] =
    for
      logEntries <- load(path)
      writeQueue <- Queue.bounded[F, ScrapeEntry](1000)
      result      = new ScrapeLog(logEntries, writeQueue, path)
    yield result

  private def load[F[_]: Sync](path: Path): F[Set[ScrapeEntry]] =
    val logger: Logger[F] = Slf4jLogger.getLogger[F]

    logger.debug(s"Loading scrape log from $path") >>
    Sync[F]
      .blocking {
        if JFiles.exists(path) then
          JFiles
            .readAllLines(path)
            .asScala
            .toList
            .flatMap(parseLine)
            .toSet
        else
          Set.empty[ScrapeEntry]
      }
      .flatTap { entries =>
        if entries.isEmpty then
          logger.info(s"No scrape log found at $path, starting fresh")
        else
          logger.info(s"Loaded ${entries.size} entries from scrape log") >>
          Sync[F].unit
      }

  private def parseLine(line: String): Option[ScrapeEntry] =
    line.trim match
      case LinePattern(svc, date, id) =>
        ScrapeService
          .values
          .find(_.toString.equalsIgnoreCase(svc))
          .map { s =>
            ScrapeEntry(s, LocalDate.parse(date), id)
          }
      case _ =>
        None

}
