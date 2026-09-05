package org.ikigaidigital.domain

import java.math.BigDecimal

/**
 * Domain-facing representation for persisted time deposit data.
 *
 * This is separate from the protected legacy TimeDeposit class,
 * which remains the compatibility contract for the existing calculator.
 */
data class TimeDepositAccount(
    val id: Int,
    val planType: String,
    val balance: BigDecimal,
    val days: Int,
    val withdrawals: List<Withdrawal> = emptyList()
)
