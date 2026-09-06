package org.ikigaidigital.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class MoneyTest {
    @Test
    fun `rounds legacy double amounts to cents with existing calculator conversion behavior`() {
        val interest = 1206.00 * 0.01 / 12

        val money = Money.roundedToCents(interest)

        assertThat(money.amount).isEqualByComparingTo("1.01")
    }

    @Test
    fun `converts legacy double balances to persisted decimal using stable decimal representation`() {
        val money = Money.persistedAmount(1207.01)

        assertThat(money.amount).isEqualByComparingTo(BigDecimal("1207.01"))
    }

    @Test
    fun `converts persisted decimal balances to legacy double`() {
        val money = Money.persistedAmount(BigDecimal("1207.01"))

        assertThat(money.toLegacyDouble()).isEqualTo(1207.01)
    }

    @Test
    fun `identifies zero amounts`() {
        assertThat(Money.persistedAmount(BigDecimal("0.00")).isZero()).isTrue()
        assertThat(Money.persistedAmount(BigDecimal("0.01")).isZero()).isFalse()
    }
}
