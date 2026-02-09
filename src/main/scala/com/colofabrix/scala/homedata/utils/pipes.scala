package com.colofabrix.scala.homedata.utils

import cats.effect.Temporal
import cats.effect.kernel.Sync
import dev.kovstas.fs2throttler.Throttler
import scala.concurrent.duration.given

package object pipes:

  def throttle[F[_]: Temporal, A](requestsPerSecond: Double) =
    val elements = Math.max(requestsPerSecond, 1.0).toLong
    val duration = Math.max(1.0 / requestsPerSecond, 1.0).toInt
    Throttler.throttle[F, A](elements, duration.second, Throttler.Shaping)

  extension [F[_]: Sync, A](stream: fs2.Stream[F, A]) {

    def every(n: Int)(f: A => F[Unit]): fs2.Stream[F, A] =
      stream
        .zipWithIndex
        .evalTap { (a, i) =>
          if i % n == 0 then f(a) else Sync[F].unit
        }
        .map((a, i) => a)

  }
