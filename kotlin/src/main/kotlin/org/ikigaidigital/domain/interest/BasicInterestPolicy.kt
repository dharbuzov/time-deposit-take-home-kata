package org.ikigaidigital.domain.interest

object BasicInterestPolicy : InterestPolicy {
    override val planType: String = "basic"

    override fun isEligible(days: Int): Boolean =
        days > 30

    override fun calculateInterest(balance: Double, days: Int): Double =
        if (isEligible(days)) {
            balance * 0.01 / 12
        } else {
            0.0
        }
}
