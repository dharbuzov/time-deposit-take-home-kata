package org.ikigaidigital.domain.interest

object PremiumInterestPolicy : InterestPolicy {
    override val planType: String = "premium"

    override fun calculateInterest(balance: Double, days: Int): Double =
        if (days > 45) {
            balance * 0.05 / 12
        } else {
            0.0
        }
}
