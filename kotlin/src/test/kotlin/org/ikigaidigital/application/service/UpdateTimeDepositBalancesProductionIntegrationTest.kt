package org.ikigaidigital.application.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.ikigaidigital.application.port.`in`.UpdateTimeDepositBalancesUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.Executors

@SpringBootTest(
    properties = [
        "app.time-deposit.update.batch-size=2",
        "app.time-deposit.update.workers=2"
    ]
)
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class UpdateTimeDepositBalancesProductionIntegrationTest {
    @Autowired
    private lateinit var updateTimeDepositBalancesUseCase: UpdateTimeDepositBalancesUseCase

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var clock: MutableTestClock

    @BeforeEach
    fun cleanDatabase() {
        clock.setInstant(Instant.parse("2026-09-05T00:00:00Z"))
        jdbcTemplate.update("DELETE FROM time_deposit_interest_accruals")
        jdbcTemplate.update("DELETE FROM withdrawals")
        jdbcTemplate.update("DELETE FROM \"timeDeposits\"")
    }

    @Test
    fun `ineligible deposits do not create claims before becoming eligible in the same month`() {
        insertTimeDeposit(1, "premium", 45, BigDecimal("1200.00"))

        val ineligible = updateTimeDepositBalancesUseCase.updateTimeDepositBalances()

        assertThat(ineligible.period).isEqualTo("2026-09")
        assertThat(ineligible.processed).isEqualTo(1)
        assertThat(ineligible.updated).isZero()
        assertThat(ineligible.alreadyProcessed).isZero()
        assertThat(ineligible.notEligible).isEqualTo(1)
        assertThat(accrualCount()).isZero()
        assertThat(balanceFor(1)).isEqualByComparingTo("1200.00")

        updateDays(1, 46)

        val eligible = updateTimeDepositBalancesUseCase.updateTimeDepositBalances()

        assertThat(eligible.updated).isEqualTo(1)
        assertThat(eligible.alreadyProcessed).isZero()
        assertThat(eligible.notEligible).isZero()
        assertThat(accrualCount()).isEqualTo(1)
        assertThat(balanceFor(1)).isEqualByComparingTo("1205.00")
    }

    @Test
    fun `basic deposit can become eligible in the same month because ineligible run creates no claim`() {
        insertTimeDeposit(1, "basic", 30, BigDecimal("1200.00"))

        val ineligible = updateTimeDepositBalancesUseCase.updateTimeDepositBalances()

        assertThat(ineligible.notEligible).isEqualTo(1)
        assertThat(accrualCount()).isZero()

        updateDays(1, 31)

        val eligible = updateTimeDepositBalancesUseCase.updateTimeDepositBalances()

        assertThat(eligible.updated).isEqualTo(1)
        assertThat(eligible.alreadyProcessed).isZero()
        assertThat(balanceFor(1)).isEqualByComparingTo("1201.00")
        assertThat(accrualCount()).isEqualTo(1)
    }

    @Test
    fun `student deposit can become eligible in the same month because ineligible run creates no claim`() {
        insertTimeDeposit(1, "student", 30, BigDecimal("1200.00"))

        val ineligible = updateTimeDepositBalancesUseCase.updateTimeDepositBalances()

        assertThat(ineligible.notEligible).isEqualTo(1)
        assertThat(accrualCount()).isZero()

        updateDays(1, 31)

        val eligible = updateTimeDepositBalancesUseCase.updateTimeDepositBalances()

        assertThat(eligible.updated).isEqualTo(1)
        assertThat(eligible.alreadyProcessed).isZero()
        assertThat(balanceFor(1)).isEqualByComparingTo("1203.00")
        assertThat(accrualCount()).isEqualTo(1)
    }

    @Test
    fun `same month retry does not apply interest twice`() {
        insertTimeDeposit(1, "basic", 31, BigDecimal("1200.00"))

        val first = updateTimeDepositBalancesUseCase.updateTimeDepositBalances()
        val second = updateTimeDepositBalancesUseCase.updateTimeDepositBalances()

        assertThat(first.updated).isEqualTo(1)
        assertThat(first.alreadyProcessed).isZero()
        assertThat(second.updated).isZero()
        assertThat(second.alreadyProcessed).isEqualTo(1)
        assertThat(balanceFor(1)).isEqualByComparingTo("1201.00")
        assertThat(accrualCount()).isEqualTo(1)
    }

    @Test
    fun `new month can process an eligible deposit again`() {
        insertTimeDeposit(1, "basic", 31, BigDecimal("1200.00"))

        updateTimeDepositBalancesUseCase.updateTimeDepositBalances()
        clock.setInstant(Instant.parse("2026-10-01T00:00:00Z"))

        val nextMonth = updateTimeDepositBalancesUseCase.updateTimeDepositBalances()

        assertThat(nextMonth.period).isEqualTo("2026-10")
        assertThat(nextMonth.updated).isEqualTo(1)
        assertThat(nextMonth.alreadyProcessed).isZero()
        assertThat(balanceFor(1)).isEqualByComparingTo("1202.00")
        assertThat(accrualCount()).isEqualTo(2)
    }

    @Test
    fun `multiple batches process all deposits once and preserve result invariant`() {
        insertTimeDeposit(1, "basic", 31, BigDecimal("1200.00"))
        insertTimeDeposit(2, "student", 365, BigDecimal("1200.00"))
        insertTimeDeposit(3, "premium", 46, BigDecimal("1200.00"))
        insertTimeDeposit(4, "premium", 45, BigDecimal("1200.00"))
        insertTimeDeposit(5, "gold", 46, BigDecimal("1200.00"))

        val result = updateTimeDepositBalancesUseCase.updateTimeDepositBalances()

        assertThat(result.processed).isEqualTo(5)
        assertThat(result.updated).isEqualTo(3)
        assertThat(result.alreadyProcessed).isZero()
        assertThat(result.notEligible).isEqualTo(2)
        assertThat(result.processed).isEqualTo(result.updated + result.alreadyProcessed + result.notEligible)
        assertThat(accrualCount()).isEqualTo(3)
    }

    @Test
    fun `concurrent HTTP calls do not double apply monthly interest`() {
        insertTimeDeposit(1, "basic", 31, BigDecimal("1200.00"))

        val executor = Executors.newFixedThreadPool(2)
        try {
            val responses = listOf(
                executor.submit<String> { postUpdate() },
                executor.submit<String> { postUpdate() }
            ).map { objectMapper.readTree(it.get()) }

            assertThat(responses.sumOf { it["updated"].asInt() }).isEqualTo(1)
            assertThat(responses.sumOf { it["alreadyProcessed"].asInt() }).isEqualTo(1)
            assertThat(balanceFor(1)).isEqualByComparingTo("1201.00")
            assertThat(accrualCount()).isEqualTo(1)
        } finally {
            executor.shutdownNow()
        }
    }

    private fun postUpdate(): String =
        mockMvc.perform(post("/time-deposits/balances"))
            .andReturn()
            .response
            .contentAsString

    private fun insertTimeDeposit(id: Int, planType: String, days: Int, balance: BigDecimal) {
        jdbcTemplate.update(
            "INSERT INTO \"timeDeposits\" (id, \"planType\", days, balance) VALUES (?, ?, ?, ?)",
            id,
            planType,
            days,
            balance
        )
    }

    private fun updateDays(id: Int, days: Int) {
        jdbcTemplate.update("UPDATE \"timeDeposits\" SET days = ? WHERE id = ?", days, id)
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
    class FixedClockConfiguration {
        @Bean
        @Primary
        fun mutableTestClock(): MutableTestClock =
            MutableTestClock()
    }

    class MutableTestClock : Clock() {
        private var currentInstant: Instant = Instant.parse("2026-09-05T00:00:00Z")

        fun setInstant(instant: Instant) {
            currentInstant = instant
        }

        override fun getZone(): ZoneId =
            ZoneId.of("UTC")

        override fun withZone(zone: ZoneId): Clock =
            this

        override fun instant(): Instant =
            currentInstant
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
