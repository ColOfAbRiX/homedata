package com.colofabrix.scala.homedata

import sttp.client4.httpclient.HttpClientSyncBackend

object Backend:
  val backend = HttpClientSyncBackend()
