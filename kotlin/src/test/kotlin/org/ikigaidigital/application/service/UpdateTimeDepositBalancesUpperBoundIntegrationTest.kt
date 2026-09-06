package org.ikigaidigital.application.service

import org.assertj.core.api.Assertions.assertThat
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
        "app.time-deposit.update.batch-size=10",
        "app.time-deposit.update.workers=1"
    ]
)
@Testcontainers(disabledWithoutDocker = true)
class UpdateTimeDepositBalancesUpperBoundIntegrationTest {
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
    fun `deposits inserted after upper bound capture are left for the next invocation`() {
        insertTimeDeposit(1, "basic", 31, BigDecimal("1200.00"))
        insertTimeDeposit(2, "basic", 31, BigDecimal("1200.00"))

        val result = updateTimeDepositBalancesUseCase.updateTimeDepositBalances()

        assertThat(result.processed).isEqualTo(2)
        assertThat(result.updated).isEqualTo(2)
        assertThat(balanceFor(1)).isEqualByComparingTo("1201.00")
        assertThat(balanceFor(2)).isEqualByComparingTo("1201.00")
        assertThat(balanceFor(3)).isEqualByComparingTo("1200.00")
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
    class UpperBoundConfiguration {
        @Bean
        @Primary
        fun fixedClock(): Clock =
            Clock.fixed(Instant.parse("2026-09-05T00:00:00Z"), ZoneOffset.UTC)

        @Bean
        @Primary
        fun upperBoundPersistencePort(jdbcTemplate: JdbcTemplate): TimeDepositPersistencePort =
            UpperBoundPersistencePort(jdbcTemplate)
    }

    private class UpperBoundPersistencePort(
        private val jdbcTemplate: JdbcTemplate
    ) : TimeDepositPersistencePort {
        private val insertedAfterUpperBound = AtomicBoolean(false)

        override fun findAllWithWithdrawals(): List<TimeDepositAccount> =
            throw UnsupportedOperationException("Not used by update-all")

        override fun findPageWithWithdrawals(request: TimeDepositPageRequest): PageResult<TimeDepositAccount> =
            throw UnsupportedOperationException("Not used by update-all")

        override fun findMaxTimeDepositId(): Int? =
            jdbcTemplate.queryForObject("SELECT MAX(id) FROM \"timeDeposits\"", Int::class.java)

        override fun findNextTimeDepositIds(lastId: Int, upperBoundId: Int, limit: Int): List<Int> {
            if (insertedAfterUpperBound.compareAndSet(false, true)) {
                jdbcTemplate.update(
                    "INSERT INTO \"timeDeposits\" (id, \"planType\", days, balance) VALUES (?, ?, ?, ?)",
                    3,
                    "basic",
                    31,
                    BigDecimal("1200.00")
                )
            }

            return jdbcTemplate.queryForList(
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
        }

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
