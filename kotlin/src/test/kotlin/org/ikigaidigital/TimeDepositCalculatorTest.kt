package org.ikigaidigital

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TimeDepositCalculatorTest {
    private val calculator = TimeDepositCalculator()

    @Test
    fun `empty list remains empty`() {
        val deposits = emptyList<TimeDeposit>()

        calculator.updateBalance(deposits)

        assertThat(deposits).isEmpty()
    }

    @Test
    fun `updates the supplied time deposit instance`() {
        val deposit = TimeDeposit(1, "basic", 1200.00, 31)
        val deposits = listOf(deposit)

        calculator.updateBalance(deposits)

        assertThat(deposits[0]).isSameAs(deposit)
        assertThat(deposit.balance).isEqualTo(1201.00)
    }

    @Test
    fun `unknown plan type receives no interest`() {
        val deposit = TimeDeposit(1, "gold", 1200.00, 60)

        calculator.updateBalance(listOf(deposit))

        assertThat(deposit.balance).isEqualTo(1200.00)
    }

    @Test
    fun `plan type matching is case sensitive`() {
        val deposit = TimeDeposit(1, "BASIC", 1200.00, 60)

        calculator.updateBalance(listOf(deposit))

        assertThat(deposit.balance).isEqualTo(1200.00)
    }

    @Test
    fun `basic plan receives no interest at 30 days`() {
        val deposit = TimeDeposit(1, "basic", 1200.00, 30)

        calculator.updateBalance(listOf(deposit))

        assertThat(deposit.balance).isEqualTo(1200.00)
    }

    @Test
    fun `basic plan receives monthly interest at 31 days`() {
        val deposit = TimeDeposit(1, "basic", 1200.00, 31)

        calculator.updateBalance(listOf(deposit))

        assertThat(deposit.balance).isEqualTo(1201.00)
    }

    @Test
    fun `basic plan receives monthly interest above 30 days`() {
        val deposit = TimeDeposit(1, "basic", 2400.00, 60)

        calculator.updateBalance(listOf(deposit))

        assertThat(deposit.balance).isEqualTo(2402.00)
    }

    @Test
    fun `student plan receives no interest at 30 days`() {
        val deposit = TimeDeposit(1, "student", 1200.00, 30)

        calculator.updateBalance(listOf(deposit))

        assertThat(deposit.balance).isEqualTo(1200.00)
    }

    @Test
    fun `student plan receives monthly interest at 31 days`() {
        val deposit = TimeDeposit(1, "student", 1200.00, 31)

        calculator.updateBalance(listOf(deposit))

        assertThat(deposit.balance).isEqualTo(1203.00)
    }

    @Test
    fun `student plan receives monthly interest at 365 days`() {
        val deposit = TimeDeposit(1, "student", 1200.00, 365)

        calculator.updateBalance(listOf(deposit))

        assertThat(deposit.balance).isEqualTo(1203.00)
    }

    @Test
    fun `student plan receives no interest at 366 days`() {
        val deposit = TimeDeposit(1, "student", 1200.00, 366)

        calculator.updateBalance(listOf(deposit))

        assertThat(deposit.balance).isEqualTo(1200.00)
    }

    @Test
    fun `premium plan receives no interest at 30 days`() {
        val deposit = TimeDeposit(1, "premium", 1200.00, 30)

        calculator.updateBalance(listOf(deposit))

        assertThat(deposit.balance).isEqualTo(1200.00)
    }

    @Test
    fun `premium plan receives no interest at 31 days`() {
        val deposit = TimeDeposit(1, "premium", 1200.00, 31)

        calculator.updateBalance(listOf(deposit))

        assertThat(deposit.balance).isEqualTo(1200.00)
    }

    @Test
    fun `premium plan receives no interest at 45 days`() {
        val deposit = TimeDeposit(1, "premium", 1200.00, 45)

        calculator.updateBalance(listOf(deposit))

        assertThat(deposit.balance).isEqualTo(1200.00)
    }

    @Test
    fun `premium plan receives monthly interest at 46 days`() {
        val deposit = TimeDeposit(1, "premium", 1200.00, 46)

        calculator.updateBalance(listOf(deposit))

        assertThat(deposit.balance).isEqualTo(1205.00)
    }

    @Test
    fun `interest is rounded to two decimals using the existing double to big decimal behavior`() {
        val deposit = TimeDeposit(1, "basic", 1206.00, 31)

        calculator.updateBalance(listOf(deposit))

        assertThat(deposit.balance).isEqualTo(1207.01)
    }
}
