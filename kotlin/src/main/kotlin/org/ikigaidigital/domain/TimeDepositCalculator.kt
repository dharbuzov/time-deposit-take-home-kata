package org.ikigaidigital.domain

import org.ikigaidigital.domain.interest.InterestPolicyResolver
import org.slf4j.LoggerFactory

class TimeDepositCalculator(
    private val interestPolicyResolver: InterestPolicyResolver = InterestPolicyResolver()
) {

    fun updateBalance(xs: List<TimeDeposit>) {
        val deposits = xs
        var updatedDeposits = 0
        var unsupportedPlans = 0

        deposits.forEach { deposit ->
            val policy = interestPolicyResolver.resolve(deposit.planType)

            if (policy == null) {
                unsupportedPlans++
                return@forEach
            }

            val interestAmountDouble = policy.calculateInterest(deposit.balance, deposit.days);
            val interestAmountCents = Money.roundedToCents(interestAmountDouble);

            if (interestAmountCents.isZero()) {
                return@forEach
            }

            deposit.balance += interestAmountDouble

            updatedDeposits++
        }

        logger.debug(
            "operation=calculate_time_deposit_interest deposits={} updatedDeposits={} unsupportedPlans={}",
            deposits.size,
            updatedDeposits,
            unsupportedPlans
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TimeDepositCalculator::class.java)
    }
}
