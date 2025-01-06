package com.colofabrix.scala.cuttlefish

import cats.effect.Async
import com.colofabrix.scala.cuttlefish.api.*
import com.colofabrix.scala.cuttlefish.model.*
import java.time.OffsetDateTime

trait CuttlefishDSL:

  extension [F[_]: Async](cuttlefishClient: CuttlefishClient[F])

    def meterConsumption(
      product: OctopusProduct,
      meterPointNumber: MeterPointNumber,
      serial: SerialNumber,
      from: Option[OffsetDateTime] = None,
      to: Option[OffsetDateTime] = None,
      pageSize: Option[Int] = None,
      orderBy: Option[String] = None,
    ): F[MeterConsumptionResponse] =
      cuttlefishClient.meterConsumption(
        MeterConsumptionRequest(
          product = product,
          meterPointNumber = meterPointNumber,
          serial = serial,
          from = from,
          to = to,
          pageSize = pageSize,
          orderBy = orderBy,
        ),
      )
