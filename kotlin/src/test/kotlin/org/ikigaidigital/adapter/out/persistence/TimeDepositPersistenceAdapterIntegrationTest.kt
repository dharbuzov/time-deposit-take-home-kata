package org.ikigaidigital.adapter.out.persistence

import org.assertj.core.api.Assertions.assertThat
import org.ikigaidigital.domain.TimeDepositBalance
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
class TimeDepositPersistenceAdapterIntegrationTest {
    @Autowired
    private lateinit var adapter: TimeDepositPersistenceAdapter

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @BeforeEach
    fun cleanDatabase() {
        jdbcTemplate.update("DELETE FROM withdrawals")
        jdbcTemplate.update("DELETE FROM timeDeposits")
    }

    @Test
    fun `retrieves multiple time deposits with associated withdrawals`() {
        insertTimeDeposit(1, "basic", 31, BigDecimal("1200.25"))
        insertTimeDeposit(2, "student", 365, BigDecimal("2200.50"))
        insertTimeDeposit(3, "premium", 46, BigDecimal("3300.75"))
        insertWithdrawal(10, 2, BigDecimal("100.10"), LocalDate.of(2026, 9, 1))
        insertWithdrawal(11, 2, BigDecimal("20.05"), LocalDate.of(2026, 9, 2))
        insertWithdrawal(12, 3, BigDecimal("300.33"), LocalDate.of(2026, 9, 3))

        val deposits = adapter.findAllWithWithdrawals()

        assertThat(deposits).hasSize(3)
        assertThat(deposits.map { it.id }).containsExactly(1, 2, 3)

        val basic = deposits[0]
        assertThat(basic.planType).isEqualTo("basic")
        assertThat(basic.days).isEqualTo(31)
        assertThat(basic.balance).isEqualByComparingTo("1200.25")
        assertThat(basic.withdrawals).isEmpty()

        val student = deposits[1]
        assertThat(student.planType).isEqualTo("student")
        assertThat(student.balance).isEqualByComparingTo("2200.50")
        assertThat(student.withdrawals.map { it.id }).containsExactly(10, 11)
        assertThat(student.withdrawals[0].amount).isEqualByComparingTo("100.10")
        assertThat(student.withdrawals[0].date).isEqualTo(LocalDate.of(2026, 9, 1))
        assertThat(student.withdrawals[1].amount).isEqualByComparingTo("20.05")

        val premium = deposits[2]
        assertThat(premium.planType).isEqualTo("premium")
        assertThat(premium.balance).isEqualByComparingTo("3300.75")
        assertThat(premium.withdrawals).hasSize(1)
        assertThat(premium.withdrawals[0].amount).isEqualByComparingTo("300.33")
    }

    @Test
    fun `replaces persisted balances without changing other deposit fields`() {
        insertTimeDeposit(1, "basic", 31, BigDecimal("1200.25"))
        insertTimeDeposit(2, "premium", 46, BigDecimal("3300.75"))

        adapter.replaceBalances(
            listOf(
                TimeDepositBalance(1, BigDecimal("1201.25")),
                TimeDepositBalance(2, BigDecimal("3314.50"))
            )
        )

        val deposits = adapter.findAllWithWithdrawals()

        assertThat(deposits[0].planType).isEqualTo("basic")
        assertThat(deposits[0].days).isEqualTo(31)
        assertThat(deposits[0].balance).isEqualByComparingTo("1201.25")
        assertThat(deposits[1].planType).isEqualTo("premium")
        assertThat(deposits[1].days).isEqualTo(46)
        assertThat(deposits[1].balance).isEqualByComparingTo("3314.50")
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
