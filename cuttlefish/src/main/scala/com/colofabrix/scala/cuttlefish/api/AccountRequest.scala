package com.colofabrix.scala.cuttlefish.api

import com.colofabrix.scala.cuttlefish.model.AccountNumber

/** Request for the account endpoint */
final case class AccountRequest(
  accountNumber: AccountNumber,
)
