package org.ikigaidigital.domain

import org.ikigaidigital.domain.interest.InterestPolicyResolver
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.math.RoundingMode

class TimeDepositCalculator {
    private val interestPolicyResolver = InterestPolicyResolver()

    fun updateBalance(xs: List<TimeDeposit>) {
        var updatedDeposits = 0
        var unsupportedPlans = 0

        for (i in xs.indices) {
            val policy = interestPolicyResolver.resolve(xs[i].planType)
            if (policy == null) {
                unsupportedPlans++
            }

            val interest = policy?.calculateInterest(xs[i].balance, xs[i].days) ?: 0.0
            val interestAmountRounded = BigDecimal(interest).setScale(2, RoundingMode.HALF_UP)
            // Increase the balance by interest amount rounded to 2 decimal places
            xs[i].balance += interestAmountRounded.toDouble()
            if (interestAmountRounded.signum() != 0) {
                updatedDeposits++
            }
        }

        logger.debug(
            "operation=calculate_time_deposit_interest deposits={} updatedDeposits={} unsupportedPlans={}",
            xs.size,
            updatedDeposits,
            unsupportedPlans
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TimeDepositCalculator::class.java)
    }
}
