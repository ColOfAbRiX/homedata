package reflux

import org.http4s.*
import io.circe.Codec

class InfluxException(message: String)
  extends Throwable(message)

class InfluxRestError(status: Status, error: ErrorResponse)
  extends InfluxException(s"$status: $error")

final case class ErrorResponse (
  code: String,
  err: Option[String],
  message: Option[String],
  op: Option[String]
) derives Codec.AsObject
