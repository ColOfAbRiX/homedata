package com.colofabrix.scala.cuttlefish.api

import com.colofabrix.scala.cuttlefish.model.ProductCode
import java.time.OffsetDateTime

/** Request for the products list endpoint */
final case class ProductsRequest(
  brand: Option[String],
  isVariable: Option[Boolean],
  isBusiness: Option[Boolean],
  isGreen: Option[Boolean],
  isPrepay: Option[Boolean],
  availableAt: Option[OffsetDateTime],
)

/** Request for the product details endpoint */
final case class ProductDetailsRequest(
  productCode: ProductCode,
  tariffsActiveAt: Option[OffsetDateTime],
)
