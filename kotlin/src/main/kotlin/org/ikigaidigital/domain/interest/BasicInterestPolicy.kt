package org.ikigaidigital.domain.interest

object BasicInterestPolicy : InterestPolicy {
    override val planType: String = "basic"

    override fun calculateInterest(balance: Double, days: Int): Double =
        if (days > 30) {
            balance * 0.01 / 12
        } else {
            0.0
        }
}
