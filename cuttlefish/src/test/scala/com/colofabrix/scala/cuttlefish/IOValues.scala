package com.colofabrix.scala.homedata.tado

import cats.effect.IO
import org.scalatest.Suite
import org.scalatest.exceptions.TestFailedException
import scala.concurrent.duration.*
import cats.effect.unsafe.IORuntime

/**
 * Mixin trait that provides helper methods for scalatest Suite, similar to scalatest's EitherValues, to test
 * IO and access its values, including exceptions
 */
trait IOValues {
  self: Suite =>

  private implicit val testRuntime: IORuntime =
    cats.effect.unsafe.implicits.global

  implicit class IOValuesExtractors[+A](self: IO[A]) {

    /** The success value contained in the monad */
    def result(timeout: FiniteDuration = 30.seconds): A =
      self
        .unsafeRunTimed(timeout)()
        .getOrElse {
          fail("Timeout while waiting for operation to complete")
        }

    /** True if the monad contains an exception */
    def isException(timeout: FiniteDuration = 30.seconds): Boolean =
      self
        .redeem(_ => true, _ => false)
        .unsafeRunTimed(timeout)
        .getOrElse {
          fail("Timeout while waiting for operation to complete")
        }

    /** The exception value contained in the monad */
    def exception(timeout: FiniteDuration = 30.seconds): Throwable =
      self
        .redeemWith(
          error => IO(error),
          _ => IO.raiseError(new TestFailedException(Some("The IO value did not contain an exception."), None, 1))
        )
        .unsafeRunTimed(timeout)
        .getOrElse {
          fail("Timeout while waiting for operation to complete")
        }

  }

}
