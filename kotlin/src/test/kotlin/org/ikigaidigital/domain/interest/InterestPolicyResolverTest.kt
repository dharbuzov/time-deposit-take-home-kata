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
}
