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

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class UpdateTimeDepositBalancesTransactionIntegrationTest {
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
    fun `update all rolls back if balance persistence fails part way through`() {
        insertTimeDeposit(1, "basic", 31, BigDecimal("1200.00"))
        insertTimeDeposit(2, "premium", 46, BigDecimal("1200.00"))

        assertThatThrownBy {
            updateTimeDepositBalancesUseCase.updateTimeDepositBalances()
        }.isInstanceOf(IllegalStateException::class.java)

        assertThat(balanceFor(1)).isEqualByComparingTo("1200.00")
        assertThat(balanceFor(2)).isEqualByComparingTo("1200.00")
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

    @TestConfiguration
    class FailingPersistencePortConfiguration {
        @Bean
        @Primary
        fun failingPersistencePort(jdbcTemplate: JdbcTemplate): TimeDepositPersistencePort =
            object : TimeDepositPersistencePort {
                override fun findAllWithWithdrawals(): List<TimeDepositAccount> =
                    listOf(
                        TimeDepositAccount(1, "basic", BigDecimal("1200.00"), 31),
                        TimeDepositAccount(2, "premium", BigDecimal("1200.00"), 46)
                    )

                override fun findPageWithWithdrawals(request: TimeDepositPageRequest): PageResult<TimeDepositAccount> =
                    PageResult(
                        content = findAllWithWithdrawals(),
                        page = request.page,
                        size = request.size,
                        totalElements = 2,
                        totalPages = 1
                    )

                override fun replaceBalances(balances: List<TimeDepositBalance>) {
                    jdbcTemplate.update(
                        "UPDATE \"timeDeposits\" SET balance = ? WHERE id = ?",
                        balances.first().balance,
                        balances.first().timeDepositId
                    )
                    throw IllegalStateException("Simulated persistence failure")
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
