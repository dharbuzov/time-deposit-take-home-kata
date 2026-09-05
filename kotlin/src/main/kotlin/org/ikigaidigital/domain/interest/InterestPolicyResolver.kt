package org.ikigaidigital.domain.interest

import org.slf4j.LoggerFactory

class InterestPolicyResolver(
    policies: List<InterestPolicy> = listOf(
        BasicInterestPolicy,
        StudentInterestPolicy,
        PremiumInterestPolicy
    )
) {
    private val policiesByPlanType = policies.associateBy { it.planType }

    fun resolve(planType: String): InterestPolicy? {
        val policy = policiesByPlanType[planType]
        if (policy == null) {
            logger.debug("operation=resolve_interest_policy status=unsupported_plan_type")
        }
        return policy
    }

    companion object {
        private val logger = LoggerFactory.getLogger(InterestPolicyResolver::class.java)
    }
}
