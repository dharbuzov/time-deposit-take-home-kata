package org.ikigaidigital

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

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class TimeDepositApplicationIntegrationTest {
    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Test
    fun `application starts and flyway creates the database schema`() {
        val timeDepositTableCount = countTablesNamed("timeDeposits")
        val withdrawalTableCount = countTablesNamed("withdrawals")
        val planTypeColumnCount = countColumnsNamed("timeDeposits", "planType")
        val timeDepositIdColumnCount = countColumnsNamed("withdrawals", "timeDepositId")

        assertThat(timeDepositTableCount).isEqualTo(1)
        assertThat(withdrawalTableCount).isEqualTo(1)
        assertThat(planTypeColumnCount).isEqualTo(1)
        assertThat(timeDepositIdColumnCount).isEqualTo(1)
    }

    private fun countTablesNamed(tableName: String): Int? =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM information_schema.tables
            WHERE table_schema = 'public'
              AND table_name = ?
            """.trimIndent(),
            Int::class.java,
            tableName
        )

    private fun countColumnsNamed(tableName: String, columnName: String): Int? =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM information_schema.columns
            WHERE table_schema = 'public'
              AND table_name = ?
              AND column_name = ?
            """.trimIndent(),
            Int::class.java,
            tableName,
            columnName
        )

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
