package org.ikigaidigital.domain.interest

interface InterestPolicy {
    val planType: String

    /**
     * Returns whether `days` satisfies the legacy business eligibility boundaries for this plan.
     *
     * Eligibility is checked before monthly accrual is claimed. That prevents an ineligible deposit from consuming
     * the current calendar month before it becomes eligible, such as Premium day 45 becoming eligible on day 46 or
     * Student day 365 becoming ineligible on day 366.
     */
    fun isEligible(days: Int): Boolean

    fun calculateInterest(balance: Double, days: Int): Double
}
