package org.ikigaidigital.domain.interest

object StudentInterestPolicy : InterestPolicy {
    override val planType: String = "student"

    override fun calculateInterest(balance: Double, days: Int): Double =
        if (days in 31..<366) {
            balance * 0.03 / 12
        } else {
            0.0
        }
}
