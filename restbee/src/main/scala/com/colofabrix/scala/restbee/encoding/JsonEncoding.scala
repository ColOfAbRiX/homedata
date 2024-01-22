package com.colofabrix.scala.restbee.encoding

import cats.effect.Sync
import cats.MonadThrow
import com.colofabrix.scala.restbee.errors.CodecError
import fs2.Pipe

/**
 * JSON Encoding
 */
trait JsonEncoder[A]:
  def encode(a: A): String

object JsonEncoder:
  def apply[A](using ev: JsonEncoder[A]): JsonEncoder[A] = ev

/**
 * JSON Decoding
 */
trait JsonDecoder[A]:
  def decode(json: String): Either[CodecError, A]
  def decodeF[F[_]: MonadThrow](json: String): F[A]
  def decodeByteStream[F[_]: Sync]: Pipe[F, Byte, A]

object JsonDecoder:
  def apply[A](using ev: JsonDecoder[A]): JsonDecoder[A] = ev
