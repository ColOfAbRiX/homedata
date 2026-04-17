package com.colofabrix.scala.homedata

import cats.effect.*
import cats.syntax.all.*
import scala.concurrent.duration.FiniteDuration

package object utils:

  extension (self: String) {
    def stdout: IO[Unit] = IO.println(self)
  }

  extension (self: FiniteDuration) {
    def sleep: IO[Unit] = IO.sleep(self)
  }
