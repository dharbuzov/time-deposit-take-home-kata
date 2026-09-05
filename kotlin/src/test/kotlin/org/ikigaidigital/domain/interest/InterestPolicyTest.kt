package org.ikigaidigital.domain.interest

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class InterestPolicyTest {
    @Test
    fun `basic policy applies only after 30 days`() {
        assertThat(BasicInterestPolicy.calculateInterest(1200.00, 30)).isEqualTo(0.0)
        assertThat(BasicInterestPolicy.calculateInterest(1200.00, 31)).isEqualTo(1.0)
        assertThat(BasicInterestPolicy.calculateInterest(2400.00, 60)).isEqualTo(2.0)
    }

    @Test
    fun `student policy applies from day 31 through day 365`() {
        assertThat(StudentInterestPolicy.calculateInterest(1200.00, 30)).isEqualTo(0.0)
        assertThat(StudentInterestPolicy.calculateInterest(1200.00, 31)).isEqualTo(3.0)
        assertThat(StudentInterestPolicy.calculateInterest(1200.00, 365)).isEqualTo(3.0)
        assertThat(StudentInterestPolicy.calculateInterest(1200.00, 366)).isEqualTo(0.0)
    }

    @Test
    fun `premium policy applies only after 45 days`() {
        assertThat(PremiumInterestPolicy.calculateInterest(1200.00, 30)).isEqualTo(0.0)
        assertThat(PremiumInterestPolicy.calculateInterest(1200.00, 31)).isEqualTo(0.0)
        assertThat(PremiumInterestPolicy.calculateInterest(1200.00, 45)).isEqualTo(0.0)
        assertThat(PremiumInterestPolicy.calculateInterest(1200.00, 46)).isEqualTo(5.0)
    }

    @Test
    fun `policy calculation preserves legacy raw double result for rounding-sensitive values`() {
        assertThat(BasicInterestPolicy.calculateInterest(1206.00, 31))
            .isEqualTo(1206.00 * 0.01 / 12)
    }
}
