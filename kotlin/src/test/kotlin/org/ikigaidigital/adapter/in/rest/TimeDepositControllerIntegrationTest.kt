package org.ikigaidigital.adapter.`in`.rest

import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.math.BigDecimal
import java.time.LocalDate

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class TimeDepositControllerIntegrationTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @BeforeEach
    fun cleanDatabase() {
        jdbcTemplate.update("DELETE FROM time_deposit_interest_accruals")
        jdbcTemplate.update("DELETE FROM withdrawals")
        jdbcTemplate.update("DELETE FROM \"timeDeposits\"")
    }

    @Test
    fun `get time deposits returns empty page using defaults`() {
        mockMvc.perform(get("/time-deposits"))
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(header().exists(CorrelationIdFilter.CORRELATION_ID_HEADER))
            .andExpect(jsonPath("$.content", hasSize<Any>(0)))
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(20))
            .andExpect(jsonPath("$.totalElements").value(0))
            .andExpect(jsonPath("$.totalPages").value(0))
    }

    @Test
    fun `get time deposits returns contract fields and withdrawals`() {
        insertTimeDeposit(1, "basic", 31, BigDecimal("1200.25"))
        insertTimeDeposit(2, "premium", 46, BigDecimal("3300.75"))
        insertWithdrawal(10, 2, BigDecimal("300.33"), LocalDate.of(2026, 9, 3))

        mockMvc.perform(get("/time-deposits"))
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.content", hasSize<Any>(2)))
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(20))
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.totalPages").value(1))
            .andExpect(jsonPath("$.content[0].id").value(1))
            .andExpect(jsonPath("$.content[0].planType").value("basic"))
            .andExpect(jsonPath("$.content[0].balance").value(1200.25))
            .andExpect(jsonPath("$.content[0].days").value(31))
            .andExpect(jsonPath("$.content[0].withdrawals", hasSize<Any>(0)))
            .andExpect(jsonPath("$.content[0].timeDepositId").doesNotExist())
            .andExpect(jsonPath("$.content[1].id").value(2))
            .andExpect(jsonPath("$.content[1].planType").value("premium"))
            .andExpect(jsonPath("$.content[1].balance").value(3300.75))
            .andExpect(jsonPath("$.content[1].days").value(46))
            .andExpect(jsonPath("$.content[1].withdrawals", hasSize<Any>(1)))
            .andExpect(jsonPath("$.content[1].withdrawals[0].id").value(10))
            .andExpect(jsonPath("$.content[1].withdrawals[0].amount").value(300.33))
            .andExpect(jsonPath("$.content[1].withdrawals[0].date").value("2026-09-03"))
            .andExpect(jsonPath("$.content[1].withdrawals[0].timeDepositId").doesNotExist())
    }

    @Test
    fun `get time deposits supports explicit page size and metadata`() {
        insertTimeDeposit(1, "basic", 31, BigDecimal("1000.00"))
        insertTimeDeposit(2, "student", 365, BigDecimal("2000.00"))
        insertTimeDeposit(3, "premium", 46, BigDecimal("3000.00"))

        mockMvc.perform(get("/time-deposits").param("page", "1").param("size", "2"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content", hasSize<Any>(1)))
            .andExpect(jsonPath("$.content[0].id").value(3))
            .andExpect(jsonPath("$.page").value(1))
            .andExpect(jsonPath("$.size").value(2))
            .andExpect(jsonPath("$.totalElements").value(3))
            .andExpect(jsonPath("$.totalPages").value(2))
    }

    @Test
    fun `get time deposits supports id descending sort`() {
        insertTimeDeposit(1, "basic", 31, BigDecimal("1000.00"))
        insertTimeDeposit(2, "student", 365, BigDecimal("2000.00"))
        insertTimeDeposit(3, "premium", 46, BigDecimal("3000.00"))

        mockMvc.perform(get("/time-deposits").param("page", "0").param("size", "2").param("sort", "id,desc"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content", hasSize<Any>(2)))
            .andExpect(jsonPath("$.content[0].id").value(3))
            .andExpect(jsonPath("$.content[1].id").value(2))
    }

    @Test
    fun `get time deposits returns empty content for page beyond result range`() {
        insertTimeDeposit(1, "basic", 31, BigDecimal("1000.00"))

        mockMvc.perform(get("/time-deposits").param("page", "1").param("size", "20"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content", hasSize<Any>(0)))
            .andExpect(jsonPath("$.page").value(1))
            .andExpect(jsonPath("$.size").value(20))
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.totalPages").value(1))
    }

    @Test
    fun `get time deposits rejects invalid pagination and sort values`() {
        mockMvc.perform(get("/time-deposits").param("page", "-1"))
            .andExpect(status().isBadRequest)

        mockMvc.perform(get("/time-deposits").param("size", "0"))
            .andExpect(status().isBadRequest)

        mockMvc.perform(get("/time-deposits").param("size", "-1"))
            .andExpect(status().isBadRequest)

        mockMvc.perform(get("/time-deposits").param("size", "101"))
            .andExpect(status().isBadRequest)

        mockMvc.perform(get("/time-deposits").param("sort", "balance,asc"))
            .andExpect(status().isBadRequest)

        mockMvc.perform(get("/time-deposits").param("sort", "id,sideways"))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `update balances returns no content and persists updated balances`() {
        insertTimeDeposit(1, "basic", 31, BigDecimal("1200.00"))
        insertTimeDeposit(2, "student", 365, BigDecimal("1200.00"))
        insertTimeDeposit(3, "premium", 46, BigDecimal("1200.00"))
        insertWithdrawal(10, 3, BigDecimal("50.00"), LocalDate.of(2026, 9, 4))

        mockMvc.perform(post("/time-deposits/balances"))
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.period").isString)
            .andExpect(jsonPath("$.processed").value(3))
            .andExpect(jsonPath("$.updated").value(3))
            .andExpect(jsonPath("$.alreadyProcessed").value(0))
            .andExpect(jsonPath("$.notEligible").value(0))

        mockMvc.perform(get("/time-deposits"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content[0].balance").value(1201.0))
            .andExpect(jsonPath("$.content[1].balance").value(1203.0))
            .andExpect(jsonPath("$.content[2].balance").value(1205.0))
            .andExpect(jsonPath("$.content[2].withdrawals", hasSize<Any>(1)))
            .andExpect(jsonPath("$.content[2].withdrawals[0].amount").value(50.0))
            .andExpect(jsonPath("$.content[2].withdrawals[0].date").value("2026-09-04"))
    }

    @Test
    fun `unsupported business endpoint variants are not exposed`() {
        mockMvc.perform(post("/time-deposits"))
            .andExpect(status().isMethodNotAllowed)

        mockMvc.perform(get("/time-deposits/balances"))
            .andExpect(status().isMethodNotAllowed)
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
