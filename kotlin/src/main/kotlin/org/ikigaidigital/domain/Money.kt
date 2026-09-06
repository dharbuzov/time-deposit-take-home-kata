package org.ikigaidigital.domain

import java.math.BigDecimal
import java.math.RoundingMode

class Money private constructor(
    val amount: BigDecimal
) {
    fun isZero(): Boolean =
        amount.signum() == 0

    fun toLegacyDouble(): Double =
        amount.toDouble()

    companion object {
        private const val CENTS_SCALE = 2

        fun legacyAmount(amount: Double): Money =
            Money(BigDecimal(amount))

        fun persistedAmount(amount: Double): Money =
            Money(BigDecimal.valueOf(amount))

        fun persistedAmount(amount: BigDecimal): Money =
            Money(amount)

        fun roundedToCents(amount: Double): Money =
            legacyAmount(amount).roundedToCents()
    }

    fun roundedToCents(): Money =
        Money(amount.setScale(CENTS_SCALE, RoundingMode.HALF_UP))
}
