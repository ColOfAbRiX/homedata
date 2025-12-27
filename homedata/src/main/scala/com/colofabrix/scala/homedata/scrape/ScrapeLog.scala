package com.colofabrix.scala.homedata.scrape

import cats.effect.*
import cats.effect.std.Queue
import cats.effect.syntax.spawn.*
import cats.implicits.*
import com.colofabrix.scala.homedata.HomedataConfig
import java.nio.file.{ Files as JFiles, Path, StandardOpenOption }
import java.time.format.DateTimeFormatter
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.regex.Matcher
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

  private val logger: Logger[F] =
    Slf4jLogger.getLogger[F]

  def contains(service: ScrapeService, timestamp: OffsetDateTime, entityId: String): Boolean =
    entries.contains(ScrapeEntry(service, timestamp.withOffsetSameInstant(ZoneOffset.UTC), entityId))

  def logEntry(service: ScrapeService, timestamp: OffsetDateTime, entityId: String): F[Unit] =
    writeQueue.offer(ScrapeEntry(service, timestamp.withOffsetSameInstant(ZoneOffset.UTC), entityId))

  def size: Int =
    entries.size

  private def runWriter: fs2.Stream[F, Nothing] =
    fs2.Stream
      .fromQueueUnterminated(writeQueue)
      .groupWithin(HomedataConfig.config.scrapeLog.batchSize, HomedataConfig.config.scrapeLog.batchWait)
      .evalMap(appendToFile)
      .drain

  private def appendToFile(batch: fs2.Chunk[ScrapeEntry]): F[Unit] =
    logger.debug(s"Writing ${batch.size} entries to scrape log") >>
    Sync[F].blocking {
      val lines =
        batch
          .toList
          .map { e =>
            val utcTimestamp = e.timestamp.withOffsetSameInstant(ZoneOffset.UTC)
            s"${e.service.toString.toLowerCase},$utcTimestamp,${e.entityId}\n"
          }
          .mkString
          .getBytes

      JFiles.write(path, lines, StandardOpenOption.APPEND, StandardOpenOption.CREATE)
      ()
    }

}

object ScrapeLog {

  private val LinePattern =
    """^(\w+),(.+?),(.+)$""".r

  def apply[F[_]: Async](path: Path): Resource[F, ScrapeLog[F]] =
    val absPath = expandPath(path)
    for
      _          <- Resource.eval(ensureDirectoryExists(absPath))
      logEntries <- Resource.eval(load(absPath))
      writeQueue <- Resource.eval(Queue.bounded[F, ScrapeEntry](1000))
      scrapeLog   = new ScrapeLog(logEntries, writeQueue, absPath)
      _          <- scrapeLog.runWriter.compile.drain.background
    yield scrapeLog

  private def expandPath(path: Path): Path =
    Path
      .of {
        path
          .toString
          .replaceFirst("^~", Matcher.quoteReplacement(System.getProperty("user.home")))
      }
      .toAbsolutePath

  private def ensureDirectoryExists[F[_]: Sync](path: Path): F[Unit] =
    Sync[F].blocking {
      val parent = path.getParent
      if parent != null && !JFiles.exists(parent) then
        JFiles.createDirectories(parent): Unit
    }

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
          logger.info(s"Loaded ${entries.size} entries from scrape log")
      }

  private def parseLine(line: String): Option[ScrapeEntry] =
    line.trim match
      case LinePattern(svc, ts, id) =>
        ScrapeService
          .values
          .find(_.toString.equalsIgnoreCase(svc))
          .map { service =>
            val date = OffsetDateTime.parse(ts).withOffsetSameInstant(ZoneOffset.UTC)
            ScrapeEntry(service, date, id)
          }
      case _ =>
        None

}
