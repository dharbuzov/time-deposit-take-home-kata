package org.ikigaidigital.adapter.out.persistence.seed

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@SpringBootTest(properties = ["app.database.seed.enabled=true"])
@Testcontainers(disabledWithoutDocker = true)
class TimeDepositDataSeederEnabledIntegrationTest {
    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Test
    fun `inserts deterministic demo data when enabled and database is empty at startup`() {
        assertThat(countRows("timeDeposits")).isEqualTo(6)
        assertThat(countRows("withdrawals")).isEqualTo(3)
        assertThat(planTypesAndDays()).containsExactlyInAnyOrder(
            "basic:30",
            "basic:31",
            "student:365",
            "student:366",
            "premium:45",
            "premium:46"
        )
    }

    private fun countRows(tableName: String): Int =
        jdbcTemplate.queryForObject("SELECT COUNT(*) FROM \"$tableName\"", Int::class.java) ?: 0

    private fun planTypesAndDays(): List<String> =
        jdbcTemplate.query(
            "SELECT \"planType\", days FROM \"timeDeposits\"",
        ) { rs, _ -> "${rs.getString("planType")}:${rs.getInt("days")}" }

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
