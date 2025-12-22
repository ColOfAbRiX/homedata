package com.colofabrix.scala.tado4s.store

import cats.effect.Sync
import cats.implicits.given
import java.nio.file.{ Files, Path, Paths }
import pureconfig.*
import pureconfig.generic.derivation.default.*

/**
 * Persisted token storage for Tado4s
 */
private[tado4s] final case class Tado4sTokenStore(refreshToken: String) derives ConfigReader

private[tado4s] object Tado4sTokenStore {

  private val tokenPath: Path =
    Paths.get(System.getProperty("user.home"), ".tado4s.conf")

  /**
   * Load the token store from ~/.tado4s.conf
   */
  def load[F[_]: Sync](): F[Option[Tado4sTokenStore]] =
    Sync[F]
      .blocking(ConfigSource.file(tokenPath.toFile).load[Tado4sTokenStore])
      .map {
        case Right(store) =>
          Some(store)
        case Left(error) =>
          System.err.println(s"Failed to parse ~/.tado4s.conf: $error")
          None
      }

  /**
   * Save the refresh token to ~/.tado4s.conf
   */
  def save[F[_]: Sync](refreshToken: String): F[Unit] =
    Sync[F].blocking {
      val content = s"""refresh-token = "$refreshToken"\n"""
      Files.writeString(tokenPath, content)
      ()
    }

  /**
   * Delete the token file ~/.tado4s.conf
   */
  def clear[F[_]: Sync](): F[Unit] =
    Sync[F].blocking {
      if Files.exists(tokenPath) then Files.delete(tokenPath)
      ()
    }

}
