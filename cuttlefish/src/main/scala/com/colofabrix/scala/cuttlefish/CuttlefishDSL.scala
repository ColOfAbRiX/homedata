package com.colofabrix.scala.cuttlefish

import cats.effect.Async
import com.colofabrix.scala.cuttlefish.api.*
import com.colofabrix.scala.cuttlefish.model.*
import java.time.OffsetDateTime

trait CuttlefishDSL:

  extension [F[_]: Async](cuttlefishClient: CuttlefishClient[F])

    def pagedMeterConsumption(
      product: OctopusProduct,
      meterPointNumber: MeterPointNumber,
      serial: SerialNumber,
      from: Option[OffsetDateTime] = None,
      to: Option[OffsetDateTime] = None,
      pageSize: Option[Int] = None,
      page: Option[Int] = None,
      orderBy: Option[String] = None,
    ): F[MeterConsumptionResponse] =
      cuttlefishClient.pagedMeterConsumption(
        MeterConsumptionRequest(
          product = product,
          meterPointNumber = meterPointNumber,
          serial = serial,
          from = from,
          to = to,
          pageSize = pageSize,
          page = page,
          orderBy = orderBy,
        ),
      )

    def meterConsumption(
      product: OctopusProduct,
      meterPointNumber: MeterPointNumber,
      serial: SerialNumber,
      from: Option[OffsetDateTime] = None,
      to: Option[OffsetDateTime] = None,
      pageSize: Option[Int] = None,
      orderBy: Option[String] = None,
      throttle: Option[Throttle] = None,
    ): fs2.Stream[F, ConsumptionResults] =
      cuttlefishClient.meterConsumption(
        request = MeterConsumptionRequest(
          product = product,
          meterPointNumber = meterPointNumber,
          serial = serial,
          from = from,
          to = to,
          pageSize = pageSize,
          page = Some(1),
          orderBy = orderBy,
        ),
        throttle = throttle,
      )
