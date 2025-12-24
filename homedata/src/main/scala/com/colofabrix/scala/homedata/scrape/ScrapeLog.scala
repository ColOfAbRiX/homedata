package com.colofabrix.scala.homedata.scrape

import cats.effect.*
import cats.effect.std.Queue
import cats.effect.syntax.spawn.*
import cats.implicits.*
import java.nio.file.{ Files as JFiles, Path, StandardOpenOption }
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import scala.concurrent.duration.*
import scala.jdk.CollectionConverters.*

enum ScrapeService {
  case Tado, Octopus
}

final case class ScrapeEntry(
  service: ScrapeService,
  timestamp: OffsetDateTime,
  entityId: String,
)

final class ScrapeLog[F[_]: Async] private (
  entries: Set[ScrapeEntry],
  writeQueue: Queue[F, ScrapeEntry],
  path: Path,
) {

  private val logger: Logger[F] = Slf4jLogger.getLogger[F]

  def contains(service: ScrapeService, timestamp: OffsetDateTime, entityId: String): Boolean =
    entries.contains(ScrapeEntry(service, timestamp, entityId))

  def logEntry(service: ScrapeService, timestamp: OffsetDateTime, entityId: String): F[Unit] =
    writeQueue.offer(ScrapeEntry(service, timestamp, entityId))

  def size: Int =
    entries.size

  private def runWriter: fs2.Stream[F, Nothing] =
    fs2.Stream
      .fromQueueUnterminated(writeQueue)
      .groupWithin(100, 100.milliseconds)
      .evalMap(appendToFile)
      .drain

  private def appendToFile(batch: fs2.Chunk[ScrapeEntry]): F[Unit] =
    logger.debug(s"Writing ${batch.size} entries to scrape log") >>
    Sync[F].blocking {
      val lines =
        batch
          .toList
          .map(e => s"${e.service.toString.toLowerCase},${e.timestamp},${e.entityId}\n")
          .mkString
          .getBytes

      JFiles.write(path, lines, StandardOpenOption.APPEND, StandardOpenOption.CREATE)
      ()
    }

}

object ScrapeLog {

  val DefaultPath: Path =
    Path.of(System.getProperty("user.home"), ".homedata", "scrape.log")

  private val LinePattern =
    """^(\w+),(.+?),(.+)$""".r

  def apply[F[_]: Async](path: Path = DefaultPath): Resource[F, ScrapeLog[F]] =
    for
      _          <- Resource.eval(ensureDirectoryExists(path))
      logEntries <- Resource.eval(load(path))
      writeQueue <- Resource.eval(Queue.bounded[F, ScrapeEntry](1000))
      scrapeLog   = new ScrapeLog(logEntries, writeQueue, path)
      _          <- scrapeLog.runWriter.compile.drain.background
    yield scrapeLog

  private def ensureDirectoryExists[F[_]: Sync](path: Path): F[Unit] =
    Sync[F].blocking {
      val parent = path.getParent
      if parent != null && !JFiles.exists(parent) then JFiles.createDirectories(parent): Unit
    }

  private def load[F[_]: Sync](path: Path): F[Set[ScrapeEntry]] =
    val logger: Logger[F] = Slf4jLogger.getLogger[F]

    logger.debug(s"Loading scrape log from $path") >>
    Sync[F]
      .blocking {
        if JFiles.exists(path) then
          JFiles.readAllLines(path).asScala.toList.flatMap(parseLine).toSet
        else
          Set.empty[ScrapeEntry]
      }
      .flatTap { entries =>
        if entries.isEmpty then logger.info(s"No scrape log found at $path, starting fresh")
        else logger.info(s"Loaded ${entries.size} entries from scrape log")
      }

  private def parseLine(line: String): Option[ScrapeEntry] =
    line.trim match
      case LinePattern(svc, ts, id) =>
        ScrapeService
          .values
          .find(_.toString.equalsIgnoreCase(svc))
          .map(s => ScrapeEntry(s, OffsetDateTime.parse(ts), id))
      case _ =>
        None

}
