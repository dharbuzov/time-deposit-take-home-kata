package org.ikigaidigital.domain

import java.math.BigDecimal

data class TimeDepositBalance(
    val timeDepositId: Int,
    val balance: BigDecimal
)
