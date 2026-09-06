package org.ikigaidigital.domain.interest

object PremiumInterestPolicy : InterestPolicy {
    override val planType: String = "premium"

    override fun isEligible(days: Int): Boolean =
        days > 45

    override fun calculateInterest(balance: Double, days: Int): Double =
        if (isEligible(days)) {
            balance * 0.05 / 12
        } else {
            0.0
        }
}
