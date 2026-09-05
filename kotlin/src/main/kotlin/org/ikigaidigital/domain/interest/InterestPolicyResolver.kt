package org.ikigaidigital.domain.interest

class InterestPolicyResolver(
    policies: List<InterestPolicy> = listOf(
        BasicInterestPolicy,
        StudentInterestPolicy,
        PremiumInterestPolicy
    )
) {
    private val policiesByPlanType = policies.associateBy { it.planType }

    fun resolve(planType: String): InterestPolicy? =
        policiesByPlanType[planType]
}
