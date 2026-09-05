package org.ikigaidigital.domain

import org.ikigaidigital.domain.interest.InterestPolicyResolver
import java.math.BigDecimal
import java.math.RoundingMode

class TimeDepositCalculator {
    private val interestPolicyResolver = InterestPolicyResolver()

    fun updateBalance(xs: List<TimeDeposit>) {
        for (i in xs.indices) {
            val interest = interestPolicyResolver.resolve(xs[i].planType)
                ?.calculateInterest(xs[i].balance, xs[i].days)
                ?: 0.0
            val a2d = BigDecimal(interest).setScale(2, RoundingMode.HALF_UP)
            xs[i].balance += a2d.toDouble()
        }
    }
}
