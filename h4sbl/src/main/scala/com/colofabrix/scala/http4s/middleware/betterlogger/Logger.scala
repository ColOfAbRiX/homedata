package com.colofabrix.scala.http4s.middleware.betterlogger

import cats.effect.*
import org.http4s.client.*

/**
 * Main entry point for the HTTP4s better logger middleware.
 *
 * This is a facade that provides backward-compatible API while delegating
 * to the new ClientLogger implementation.
 */
object Logger:

  /**
   * Creates a logging middleware for an HTTP4s client.
   */
  def apply[F[_]: Async](client: Client[F]): Client[F] =
    val config = LogConfig(redactHeaders = true)
    ClientLogger(config)(client)

  /**
   * Creates a logging middleware for an HTTP4s client.
   */
  def apply[F[_]: Async](redactHeaders: Boolean = true)(client: Client[F]): Client[F] =
    val config = LogConfig(redactHeaders = redactHeaders)
    ClientLogger(config)(client)

  /**
   * Creates a logging middleware with full configuration.
   */
  def withConfig[F[_]: Async](config: LogConfig)(client: Client[F]): Client[F] =
    ClientLogger(config)(client)
