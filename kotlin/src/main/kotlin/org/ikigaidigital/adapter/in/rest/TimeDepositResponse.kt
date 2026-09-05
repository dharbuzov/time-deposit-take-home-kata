package org.ikigaidigital.adapter.`in`.rest

import java.math.BigDecimal
import java.time.LocalDate

data class TimeDepositResponse(
    val id: Int,
    val planType: String,
    val balance: BigDecimal,
    val days: Int,
    val withdrawals: List<WithdrawalResponse>
)

data class WithdrawalResponse(
    val id: Int,
    val amount: BigDecimal,
    val date: LocalDate
)
