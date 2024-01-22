package reflux

import org.http4s.*

class InfluxException(status: Status, message: String) extends Throwable(s"$status: $message")
