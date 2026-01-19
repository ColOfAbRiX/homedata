package com.colofabrix.scala.tado4s.store

import cats.effect.Sync
import cats.implicits.given
import com.colofabrix.scala.tado4s.TadoConfig
import com.typesafe.config.ConfigRenderOptions
import java.nio.file.{Files, Path}
import pureconfig.*

object Tado4sTokenStore {

  private val tokenPath: Path =
    TadoConfig.config.tokenPath

  /**
   * Load the token store
   */
  def load[F[_]: Sync](): F[Option[TadoRefreshToken]] =
    Sync[F]
      .blocking(ConfigSource.file(tokenPath.toFile).load[TadoRefreshToken])
      .map {
        case Right(token) =>
          Some(token)
        case Left(error) =>
          System.err.println(s"Failed to parse ${tokenPath}: $error")
          None
      }

  /**
   * Save the refresh token
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
   * Delete the token file
   */
  def clear[F[_]: Sync](): F[Unit] =
    Sync[F].blocking {
      if Files.exists(tokenPath) then Files.delete(tokenPath)
      ()
    }

}
