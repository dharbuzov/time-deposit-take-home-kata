package org.ikigaidigital.domain.interest

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class InterestPolicyResolverTest {
    private val resolver = InterestPolicyResolver()

    @Test
    fun `resolves known lowercase plan types explicitly`() {
        assertThat(resolver.resolve("basic")).isSameAs(BasicInterestPolicy)
        assertThat(resolver.resolve("student")).isSameAs(StudentInterestPolicy)
        assertThat(resolver.resolve("premium")).isSameAs(PremiumInterestPolicy)
    }

    @Test
    fun `does not resolve unknown or differently cased plan types`() {
        assertThat(resolver.resolve("gold")).isNull()
        assertThat(resolver.resolve("BASIC")).isNull()
    }

    @Test
    fun `evaluates eligibility using legacy day boundaries`() {
        assertThat(resolver.isEligible("basic", 30)).isFalse()
        assertThat(resolver.isEligible("basic", 31)).isTrue()

        assertThat(resolver.isEligible("student", 30)).isFalse()
        assertThat(resolver.isEligible("student", 31)).isTrue()
        assertThat(resolver.isEligible("student", 365)).isTrue()
        assertThat(resolver.isEligible("student", 366)).isFalse()

        assertThat(resolver.isEligible("premium", 45)).isFalse()
        assertThat(resolver.isEligible("premium", 46)).isTrue()

        assertThat(resolver.isEligible("gold", 46)).isFalse()
    }
}
