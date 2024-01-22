package com.colofabrix.scala.timeflux.circe

import cats.effect.Sync
import cats.implicits.given
import cats.MonadThrow
import com.colofabrix.scala.restbee.encoding.*
import com.colofabrix.scala.restbee.errors.CodecError
import fs2.Pipe
import io.circe.{ Decoder, Encoder }
import io.circe.fs2.{ byteStreamParser, decoder }
import io.circe.parser.{ decode => circeDecode }

/**
 * Data Encoding with Circe
 */
object CirceJsonCodecs:

  given circeJsonEncoder[A: Encoder]: JsonEncoder[A] with
    def encode(a: A): String =
      Encoder[A]
        .apply(a)
        .noSpacesSortKeys

  given circeJsonDecoder[A: Decoder]: JsonDecoder[A] with
    def decode(json: String): Either[CodecError, A] =
      circeDecode[A](json).leftMap(e => CodecError(e.toString))

    def decodeF[F[_]: MonadThrow](json: String): F[A] =
      circeDecode[A](json) match {
        case Right(value) => MonadThrow[F].pure(value)
        case Left(error)  => MonadThrow[F].raiseError(CodecError("Circe decoding error", Some(error)))
      }

    def decodeByteStream[F[_]: Sync]: Pipe[F, Byte, A] =
      stream =>
        stream
          .through(byteStreamParser)
          .through(decoder[F, A])
          .adaptError {
            case error => CodecError("Circe decoding error", Some(error))
          }
