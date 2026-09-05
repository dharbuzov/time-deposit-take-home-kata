package org.ikigaidigital.domain

import java.math.BigDecimal
import java.time.LocalDate

/**
 * Domain-facing representation for persisted withdrawal data.
 */
data class Withdrawal(
    val id: Int,
    val amount: BigDecimal,
    val date: LocalDate
)
