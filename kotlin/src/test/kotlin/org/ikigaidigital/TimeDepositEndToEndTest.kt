package org.ikigaidigital

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.ikigaidigital.adapter.`in`.rest.CorrelationIdFilter
import org.ikigaidigital.adapter.`in`.rest.TimeDepositController
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.servlet.mvc.method.RequestMappingInfo
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.math.BigDecimal
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class TimeDepositEndToEndTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var requestMappingHandlerMapping: RequestMappingHandlerMapping

    @BeforeEach
    fun cleanDatabase() {
        jdbcTemplate.update("DELETE FROM time_deposit_interest_accruals")
        jdbcTemplate.update("DELETE FROM withdrawals")
        jdbcTemplate.update("DELETE FROM \"timeDeposits\"")
    }

    @Test
    fun `empty database supports read and balance update through real stack`() {
        mockMvc.perform(get("/time-deposits"))
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(header().exists(CorrelationIdFilter.CORRELATION_ID_HEADER))
            .andExpect(content().json("""{"content":[],"page":0,"size":20,"totalElements":0,"totalPages":0}"""))

        mockMvc.perform(post("/time-deposits/balances"))
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(content().json(
                """{"processed":0,"updated":0,"alreadyProcessed":0,"notEligible":0}""",
                false
            ))

        assertThat(countRows("timeDeposits")).isZero()
        assertThat(countRows("withdrawals")).isZero()
    }

    @Test
    fun `get time deposits returns only the public contract shape`() {
        insertTimeDeposit(1, "basic", 31, BigDecimal("1200.25"))
        insertTimeDeposit(2, "premium", 46, BigDecimal("3300.75"))
        insertWithdrawal(10, 2, BigDecimal("300.33"), LocalDate.of(2026, 9, 3))

        val body = mockMvc.perform(get("/time-deposits"))
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andReturn()
            .response
            .contentAsString

        val json = objectMapper.readTree(body)

        assertThat(json.isObject).isTrue()
        assertThat(fieldNames(json)).containsExactlyInAnyOrder(
            "content",
            "page",
            "size",
            "totalElements",
            "totalPages"
        )
        assertThat(json["page"].asInt()).isEqualTo(0)
        assertThat(json["size"].asInt()).isEqualTo(20)
        assertThat(json["totalElements"].asLong()).isEqualTo(2)
        assertThat(json["totalPages"].asInt()).isEqualTo(1)

        val content = json["content"]
        assertThat(content.isArray).isTrue()
        assertThat(content).hasSize(2)
        assertThat(fieldNames(content[0])).containsExactlyInAnyOrder(
            "id",
            "planType",
            "balance",
            "days",
            "withdrawals"
        )
        assertThat(content[0]["balance"].isNumber).isTrue()
        assertThat(content[0]["withdrawals"]).isEmpty()
        assertThat(content[0].has("timeDepositId")).isFalse()
        assertThat(content[0].has("version")).isFalse()

        assertThat(content[1]["id"].asInt()).isEqualTo(2)
        assertThat(content[1]["planType"].asText()).isEqualTo("premium")
        assertThat(content[1]["balance"].isNumber).isTrue()
        assertThat(content[1]["days"].asInt()).isEqualTo(46)
        assertThat(content[1]["withdrawals"]).hasSize(1)

        val withdrawal = content[1]["withdrawals"][0]
        assertThat(fieldNames(withdrawal)).containsExactlyInAnyOrder("id", "amount", "date")
        assertThat(withdrawal["id"].asInt()).isEqualTo(10)
        assertThat(withdrawal["amount"].isNumber).isTrue()
        assertThat(withdrawal["date"].asText()).isEqualTo("2026-09-03")
        assertThat(withdrawal.has("timeDepositId")).isFalse()
    }

    @Test
    fun `update balances through HTTP persists calculator results and leaves withdrawals unchanged`() {
        insertTimeDeposit(1, "basic", 31, BigDecimal("1200.00"))
        insertTimeDeposit(2, "student", 365, BigDecimal("1200.00"))
        insertTimeDeposit(3, "premium", 45, BigDecimal("1200.00"))
        insertTimeDeposit(4, "premium", 46, BigDecimal("1200.00"))
        insertTimeDeposit(5, "gold", 46, BigDecimal("1200.00"))
        insertWithdrawal(10, 4, BigDecimal("50.00"), LocalDate.of(2026, 9, 4))

        val originalWithdrawalCount = countRows("withdrawals")
        val originalWithdrawalAmount = withdrawalAmount(10)
        val originalWithdrawalDate = withdrawalDate(10)

        mockMvc.perform(post("/time-deposits/balances"))
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))

        val body = mockMvc.perform(get("/time-deposits"))
            .andExpect(status().isOk)
            .andReturn()
            .response
            .contentAsString
        val json = objectMapper.readTree(body)
        val balancesById = json["content"].associate { it["id"].asInt() to it["balance"].decimalValue() }

        assertThat(balancesById[1]).isEqualByComparingTo("1201.00")
        assertThat(balancesById[2]).isEqualByComparingTo("1203.00")
        assertThat(balancesById[3]).isEqualByComparingTo("1200.00")
        assertThat(balancesById[4]).isEqualByComparingTo("1205.00")
        assertThat(balancesById[5]).isEqualByComparingTo("1200.00")

        assertThat(balanceFor(1)).isEqualByComparingTo("1201.00")
        assertThat(balanceFor(2)).isEqualByComparingTo("1203.00")
        assertThat(balanceFor(3)).isEqualByComparingTo("1200.00")
        assertThat(balanceFor(4)).isEqualByComparingTo("1205.00")
        assertThat(balanceFor(5)).isEqualByComparingTo("1200.00")
        assertThat(countRows("withdrawals")).isEqualTo(originalWithdrawalCount)
        assertThat(withdrawalAmount(10)).isEqualByComparingTo(originalWithdrawalAmount)
        assertThat(withdrawalDate(10)).isEqualTo(originalWithdrawalDate)
    }

    @Test
    fun `correlation id is propagated or generated through real endpoints`() {
        mockMvc.perform(
            get("/time-deposits")
                .header(CorrelationIdFilter.CORRELATION_ID_HEADER, "test-correlation-id")
        )
            .andExpect(status().isOk)
            .andExpect(header().string(CorrelationIdFilter.CORRELATION_ID_HEADER, "test-correlation-id"))

        val generatedCorrelationId = mockMvc.perform(get("/time-deposits"))
            .andExpect(status().isOk)
            .andExpect(header().exists(CorrelationIdFilter.CORRELATION_ID_HEADER))
            .andReturn()
            .response
            .getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER)

        assertThat(generatedCorrelationId).isNotBlank()
    }

    @Test
    fun `business controller exposes exactly the two contract endpoints`() {
        val controllerMappings = requestMappingHandlerMapping.handlerMethods
            .filterValues { it.beanType == TimeDepositController::class.java }
            .keys
            .flatMap(::endpointSignatures)

        assertThat(controllerMappings).containsExactlyInAnyOrder(
            "GET /time-deposits",
            "POST /time-deposits/balances"
        )
    }

    @Test
    fun `openapi contract documents the implemented business endpoints`() {
        val contract = Files.readString(openApiContractPath())

        assertThat(contract).contains("/time-deposits:")
        assertThat(contract).contains("get:")
        assertThat(contract).contains("'200':")
        assertThat(contract).contains("/time-deposits/balances:")
        assertThat(contract).contains("post:")
        assertThat(contract).contains("'200':")
        assertThat(contract).contains("- name: page")
        assertThat(contract).contains("- name: size")
        assertThat(contract).contains("- name: sort")
        assertThat(contract).contains("TimeDepositPageResponse:")
        assertThat(contract).contains("TimeDepositResponse:")
        assertThat(contract).contains("WithdrawalResponse:")
        assertThat(contract).contains("UpdateTimeDepositBalancesResponse:")
    }

    private fun endpointSignatures(mappingInfo: RequestMappingInfo): List<String> {
        val paths = mappingInfo.pathPatternsCondition?.patterns
            ?.map { it.patternString }
            ?: mappingInfo.patternsCondition?.patterns?.toList()
            ?: emptyList()

        return paths.flatMap { path ->
            mappingInfo.methodsCondition.methods.map { method -> "${method.name} $path" }
        }
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

    private fun countRows(tableName: String): Int =
        jdbcTemplate.queryForObject("SELECT COUNT(*) FROM \"${tableName}\"", Int::class.java) ?: 0

    private fun balanceFor(id: Int): BigDecimal? =
        jdbcTemplate.queryForObject(
            "SELECT balance FROM \"timeDeposits\" WHERE id = ?",
            BigDecimal::class.java,
            id
        )

    private fun withdrawalAmount(id: Int): BigDecimal? =
        jdbcTemplate.queryForObject(
            "SELECT amount FROM withdrawals WHERE id = ?",
            BigDecimal::class.java,
            id
        )

    private fun withdrawalDate(id: Int): LocalDate? =
        jdbcTemplate.queryForObject(
            "SELECT date FROM withdrawals WHERE id = ?",
            LocalDate::class.java,
            id
        )

    private fun fieldNames(json: JsonNode): Set<String> =
        json.fieldNames().asSequence().toSet()

    private fun openApiContractPath(): Path =
        generateSequence(Path.of("").toAbsolutePath()) { it.parent }
            .map { it.resolve("docs/openapi.yaml") }
            .firstOrNull(Files::exists)
            ?: error("docs/openapi.yaml was not found")

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
