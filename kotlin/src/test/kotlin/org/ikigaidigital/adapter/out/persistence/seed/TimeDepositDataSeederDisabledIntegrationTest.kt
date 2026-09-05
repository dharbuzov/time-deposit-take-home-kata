package org.ikigaidigital.adapter.out.persistence.seed

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@SpringBootTest(properties = ["app.database.seed.enabled=false"])
@Testcontainers(disabledWithoutDocker = true)
class TimeDepositDataSeederDisabledIntegrationTest {
    @Autowired
    private lateinit var applicationContext: ApplicationContext

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Test
    fun `does not insert demo data when disabled`() {
        assertThat(applicationContext.getBeansOfType(TimeDepositDataSeeder::class.java)).isEmpty()
        assertThat(countRows("timeDeposits")).isZero()
        assertThat(countRows("withdrawals")).isZero()
    }

    private fun countRows(tableName: String): Int =
        jdbcTemplate.queryForObject("SELECT COUNT(*) FROM \"$tableName\"", Int::class.java) ?: 0

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
