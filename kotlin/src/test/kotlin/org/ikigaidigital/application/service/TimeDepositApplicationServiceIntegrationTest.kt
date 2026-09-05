package org.ikigaidigital.application.service

import org.assertj.core.api.Assertions.assertThat
import org.ikigaidigital.application.port.`in`.GetTimeDepositsUseCase
import org.ikigaidigital.application.port.`in`.UpdateTimeDepositBalancesUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.math.BigDecimal
import java.time.LocalDate

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class TimeDepositApplicationServiceIntegrationTest {
    @Autowired
    private lateinit var getTimeDepositsUseCase: GetTimeDepositsUseCase

    @Autowired
    private lateinit var updateTimeDepositBalancesUseCase: UpdateTimeDepositBalancesUseCase

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @BeforeEach
    fun cleanDatabase() {
        jdbcTemplate.update("DELETE FROM withdrawals")
        jdbcTemplate.update("DELETE FROM \"timeDeposits\"")
    }

    @Test
    fun `get all returns empty result when no deposits exist`() {
        val deposits = getTimeDepositsUseCase.getTimeDeposits()

        assertThat(deposits).isEmpty()
    }

    @Test
    fun `get all returns deposits with withdrawals and monetary values`() {
        insertTimeDeposit(1, "basic", 31, BigDecimal("1200.25"))
        insertTimeDeposit(2, "premium", 46, BigDecimal("3300.75"))
        insertWithdrawal(10, 2, BigDecimal("300.33"), LocalDate.of(2026, 9, 3))

        val deposits = getTimeDepositsUseCase.getTimeDeposits()

        assertThat(deposits.map { it.id }).containsExactly(1, 2)
        assertThat(deposits[0].balance).isEqualByComparingTo("1200.25")
        assertThat(deposits[0].withdrawals).isEmpty()
        assertThat(deposits[1].balance).isEqualByComparingTo("3300.75")
        assertThat(deposits[1].withdrawals).hasSize(1)
        assertThat(deposits[1].withdrawals[0].amount).isEqualByComparingTo("300.33")
        assertThat(deposits[1].withdrawals[0].date).isEqualTo(LocalDate.of(2026, 9, 3))
    }

    @Test
    fun `update all does nothing when no deposits exist`() {
        updateTimeDepositBalancesUseCase.updateTimeDepositBalances()

        assertThat(getTimeDepositsUseCase.getTimeDeposits()).isEmpty()
    }

    @Test
    fun `update all persists calculated balances and leaves withdrawals unchanged`() {
        insertTimeDeposit(1, "basic", 31, BigDecimal("1200.00"))
        insertTimeDeposit(2, "student", 365, BigDecimal("1200.00"))
        insertTimeDeposit(3, "premium", 46, BigDecimal("1200.00"))
        insertTimeDeposit(4, "gold", 46, BigDecimal("1200.00"))
        insertWithdrawal(10, 3, BigDecimal("50.00"), LocalDate.of(2026, 9, 4))

        updateTimeDepositBalancesUseCase.updateTimeDepositBalances()

        val deposits = getTimeDepositsUseCase.getTimeDeposits()

        assertThat(deposits[0].balance).isEqualByComparingTo("1201.00")
        assertThat(deposits[1].balance).isEqualByComparingTo("1203.00")
        assertThat(deposits[2].balance).isEqualByComparingTo("1205.00")
        assertThat(deposits[3].balance).isEqualByComparingTo("1200.00")
        assertThat(deposits[2].withdrawals).hasSize(1)
        assertThat(deposits[2].withdrawals[0].amount).isEqualByComparingTo("50.00")
    }

    private fun insertTimeDeposit(id: Int, planType: String, days: Int, balance: BigDecimal) {
        jdbcTemplate.update(
            "INSERT INTO \"timeDeposits\" (id, \"planType\", days, balance) VALUES (?, ?, ?, ?)",
            id,
            planType,
            days,
            balance
        )
    }

    private fun insertWithdrawal(id: Int, timeDepositId: Int, amount: BigDecimal, date: LocalDate) {
        jdbcTemplate.update(
            "INSERT INTO withdrawals (id, \"timeDepositId\", amount, date) VALUES (?, ?, ?, ?)",
            id,
            timeDepositId,
            amount,
            date
        )
    }

    companion object {
        @Container
        @JvmStatic
        private val postgres = PostgreSQLContainer<Nothing>("postgres:16-alpine")

        @DynamicPropertySource
        @JvmStatic
        fun databaseProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
        }
    }
}
