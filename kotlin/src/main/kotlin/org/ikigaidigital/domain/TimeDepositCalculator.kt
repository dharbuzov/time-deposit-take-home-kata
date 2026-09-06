package org.ikigaidigital.domain

import org.ikigaidigital.domain.interest.InterestPolicyResolver
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.math.RoundingMode

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

            val interest = roundToCents(policy.calculateInterest(deposit.balance, deposit.days))

            if (interest.signum() == 0) {
                return@forEach
            }

            applyInterest(deposit, interest)
            updatedDeposits++
        }

        logger.debug(
            "operation=calculate_time_deposit_interest deposits={} updatedDeposits={} unsupportedPlans={}",
            deposits.size,
            updatedDeposits,
            unsupportedPlans
        )
    }

    private fun roundToCents(amount: Double): BigDecimal =
        BigDecimal(amount).setScale(2, RoundingMode.HALF_UP)

    private fun applyInterest(deposit: TimeDeposit, interest: BigDecimal) {
        deposit.balance += interest.toDouble()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TimeDepositCalculator::class.java)
    }
}
