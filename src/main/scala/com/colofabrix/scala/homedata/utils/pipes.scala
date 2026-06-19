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
