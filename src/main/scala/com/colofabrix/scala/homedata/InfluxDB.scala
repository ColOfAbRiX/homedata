package com.colofabrix.scala.homedata

import com.colofabrix.scala.timeflux.config.*
import org.http4s.Uri

object InfluxDB:
  val serverUri: Uri =
    Uri.unsafeFromString("http://127.0.0.1:8086")

  val organizationId: OrganizationId =
    OrganizationId("dd87acad77d7e016")

  val authToken: AuthToken =
    AuthToken("VRB6CbkkF4CcAG8dKHu56D_t8kA4IumEMI5-QazdKz0ry3vArEe3tOCSw3YvgTnHinIEicUXpEC-5rI3Zu1rjQ==")

  val octopusBucket: String =
    "octopus"
