package com.colofabrix.scala.tado4s.store

import cats.effect.Sync
import cats.implicits.given
import com.typesafe.config.ConfigRenderOptions
import java.nio.file.{Files, Path, Paths}
import pureconfig.*

/**
 * Persisted token storage for Tado4s - manages ~/.tado4s.conf
 */
object Tado4sTokenStore:

  private val tokenPath: Path =
    Paths.get(System.getProperty("user.home"), ".tado4s.conf")

  /**
   * Load the token store from ~/.tado4s.conf
   */
  def load[F[_]: Sync](): F[Option[TadoRefreshToken]] =
    Sync[F]
      .blocking(ConfigSource.file(tokenPath.toFile).load[TadoRefreshToken])
      .map {
        case Right(token) =>
          Some(token)
        case Left(error) =>
          System.err.println(s"Failed to parse ~/.tado4s.conf: $error")
          None
      }

  /**
   * Save the refresh token to ~/.tado4s.conf
   */
  def save[F[_]: Sync](token: TadoRefreshToken): F[Unit] =
    Sync[F].blocking {
      val configValue = ConfigWriter[TadoRefreshToken].to(token)
      val renderOptions = ConfigRenderOptions.concise().setFormatted(true).setJson(false)
      val content = configValue.render(renderOptions)
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
