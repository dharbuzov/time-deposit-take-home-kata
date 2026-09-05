package org.ikigaidigital.domain.interest

interface InterestPolicy {
    val planType: String

    fun calculateInterest(balance: Double, days: Int): Double
}
