package org.ikigaidigital.application.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.ikigaidigital.application.port.`in`.PageResult
import org.ikigaidigital.application.port.`in`.TimeDepositPageRequest
import org.ikigaidigital.application.port.`in`.UpdateTimeDepositBalancesUseCase
import org.ikigaidigital.application.port.out.TimeDepositPersistencePort
import org.ikigaidigital.domain.TimeDepositAccount
import org.ikigaidigital.domain.TimeDepositBalance
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.math.BigDecimal
import java.sql.Timestamp
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.concurrent.atomic.AtomicBoolean

@SpringBootTest(
    properties = [
        "app.time-deposit.update.batch-size=1",
        "app.time-deposit.update.workers=1"
    ]
)
@Testcontainers(disabledWithoutDocker = true)
class UpdateTimeDepositBalancesRetryIntegrationTest {
    @Autowired
    private lateinit var updateTimeDepositBalancesUseCase: UpdateTimeDepositBalancesUseCase

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @BeforeEach
    fun cleanDatabase() {
        jdbcTemplate.update("DELETE FROM time_deposit_interest_accruals")
        jdbcTemplate.update("DELETE FROM withdrawals")
        jdbcTemplate.update("DELETE FROM \"timeDeposits\"")
    }

    @Test
    fun `same month retry after partial failure skips committed batches and retries rolled back batch`() {
        insertTimeDeposit(1, "basic", 31, BigDecimal("1200.00"))
        insertTimeDeposit(2, "premium", 46, BigDecimal("1200.00"))

        assertThatThrownBy {
            updateTimeDepositBalancesUseCase.updateTimeDepositBalances()
        }.isInstanceOf(IllegalStateException::class.java)

        assertThat(balanceFor(1)).isEqualByComparingTo("1201.00")
        assertThat(balanceFor(2)).isEqualByComparingTo("1200.00")
        assertThat(accrualCount()).isEqualTo(1)

        val retry = updateTimeDepositBalancesUseCase.updateTimeDepositBalances()

        assertThat(retry.period).isEqualTo("2026-09")
        assertThat(retry.processed).isEqualTo(2)
        assertThat(retry.updated).isEqualTo(1)
        assertThat(retry.alreadyProcessed).isEqualTo(1)
        assertThat(retry.notEligible).isZero()
        assertThat(balanceFor(1)).isEqualByComparingTo("1201.00")
        assertThat(balanceFor(2)).isEqualByComparingTo("1205.00")
        assertThat(accrualCount()).isEqualTo(2)
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

    private fun balanceFor(id: Int): BigDecimal? =
        jdbcTemplate.queryForObject(
            "SELECT balance FROM \"timeDeposits\" WHERE id = ?",
            BigDecimal::class.java,
            id
        )

    private fun accrualCount(): Int =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM time_deposit_interest_accruals",
            Int::class.java
        ) ?: 0

    @TestConfiguration
    class RetryConfiguration {
        @Bean
        @Primary
        fun fixedClock(): Clock =
            Clock.fixed(Instant.parse("2026-09-05T00:00:00Z"), ZoneOffset.UTC)

        @Bean
        @Primary
        fun flakyPersistencePort(jdbcTemplate: JdbcTemplate): TimeDepositPersistencePort =
            FlakyPersistencePort(jdbcTemplate)
    }

    private class FlakyPersistencePort(
        private val jdbcTemplate: JdbcTemplate
    ) : TimeDepositPersistencePort {
        private val failedSecondBatch = AtomicBoolean(false)

        override fun findAllWithWithdrawals(): List<TimeDepositAccount> =
            throw UnsupportedOperationException("Not used by update-all")

        override fun findPageWithWithdrawals(request: TimeDepositPageRequest): PageResult<TimeDepositAccount> =
            throw UnsupportedOperationException("Not used by update-all")

        override fun findMaxTimeDepositId(): Int? =
            jdbcTemplate.queryForObject("SELECT MAX(id) FROM \"timeDeposits\"", Int::class.java)

        override fun findNextTimeDepositIds(lastId: Int, upperBoundId: Int, limit: Int): List<Int> =
            jdbcTemplate.queryForList(
                """
                SELECT id
                FROM "timeDeposits"
                WHERE id > ?
                  AND id <= ?
                ORDER BY id
                LIMIT ?
                """.trimIndent(),
                Int::class.java,
                lastId,
                upperBoundId,
                limit
            )

        override fun findByIds(timeDepositIds: List<Int>): List<TimeDepositAccount> =
            timeDepositIds.map { id ->
                jdbcTemplate.queryForObject(
                    """
                    SELECT id, "planType", days, balance
                    FROM "timeDeposits"
                    WHERE id = ?
                    """.trimIndent(),
                    { rs, _ ->
                        TimeDepositAccount(
                            id = rs.getInt("id"),
                            planType = rs.getString("planType"),
                            balance = rs.getBigDecimal("balance"),
                            days = rs.getInt("days")
                        )
                    },
                    id
                ) ?: error("Time deposit $id was not found")
            }

        override fun tryClaimMonthlyInterest(
            timeDepositId: Int,
            accrualPeriod: String,
            createdAt: Instant
        ): Boolean =
            jdbcTemplate.update(
                """
                INSERT INTO time_deposit_interest_accruals(time_deposit_id, accrual_period, created_at)
                VALUES (?, ?, ?)
                ON CONFLICT (time_deposit_id, accrual_period) DO NOTHING
                """.trimIndent(),
                timeDepositId,
                accrualPeriod,
                Timestamp.from(createdAt)
            ) == 1

        override fun replaceBalances(balances: List<TimeDepositBalance>) {
            balances.forEach { balance ->
                jdbcTemplate.update(
                    "UPDATE \"timeDeposits\" SET balance = ? WHERE id = ?",
                    balance.balance,
                    balance.timeDepositId
                )
            }
            if (balances.any { it.timeDepositId == 2 } && failedSecondBatch.compareAndSet(false, true)) {
                throw IllegalStateException("Simulated second batch failure")
            }
        }
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
