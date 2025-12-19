package com.colofabrix.scala.tado4s.security

import cats.effect.MonadCancelThrow
import cats.effect.kernel.Resource
import cats.implicits.given
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import org.http4s.*
import org.http4s.client.Client

object SSLValidationClient:

  def apply[F[_]: MonadCancelThrow](ignoreSsl: Boolean)(httpClient: Client[F]): Client[F] =
    Client[F] { request =>
      Resource.pure {
        if ignoreSsl then SSLContext.setDefault(trustAllSslContext)
        else SSLContext.setDefault(defaultSslContext)
      } >>
      httpClient.run(request)
    }

  private lazy val defaultSslContext =
    SSLContext.getDefault()

  private lazy val trustAllSslContext = {
    val trustManager =
      new X509TrustManager {
        def getAcceptedIssuers(): Array[X509Certificate] =
          Array.empty
        def checkClientTrusted(certs: Array[X509Certificate], authType: String): Unit =
          ()
        def checkServerTrusted(certs: Array[X509Certificate], authType: String): Unit =
          ()
      }

    val sslContext = SSLContext.getInstance("TLS")
    sslContext.init(null, Array[TrustManager](trustManager), new SecureRandom())
    sslContext
  }
