package org.ikigaidigital.adapter.out.persistence.seed

import org.ikigaidigital.adapter.out.persistence.TimeDepositEntity
import org.ikigaidigital.adapter.out.persistence.TimeDepositJpaRepository
import org.ikigaidigital.adapter.out.persistence.WithdrawalEntity
import org.ikigaidigital.adapter.out.persistence.WithdrawalJpaRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate

@Component
@ConditionalOnProperty(
    prefix = "app.database.seed",
    name = ["enabled"],
    havingValue = "true"
)
class TimeDepositDataSeeder(
    private val timeDepositRepository: TimeDepositJpaRepository,
    private val withdrawalRepository: WithdrawalJpaRepository
) : ApplicationRunner {

    /**
     * Seeds the database with demo time deposits and withdrawals.
     */
    @Transactional
    override fun run(args: ApplicationArguments) {
        if (timeDepositRepository.count() > 0) {
            logger.info("operation=demo_seed status=skipped reason=data_exists")
            return
        }

        val deposits = timeDepositRepository.saveAll(demoDeposits())
        val withdrawals = withdrawalRepository.saveAll(demoWithdraws(deposits))

        logger.info(
            "operation=demo_seed deposits={} withdrawals={} status=success",
            deposits.size,
            withdrawals.size
        )
    }

    private fun demoDeposits(): List<TimeDepositEntity> =
        listOf(
            TimeDepositEntity("basic", 30, BigDecimal("1200.00")),
            TimeDepositEntity("basic", 31, BigDecimal("1200.00")),
            TimeDepositEntity("student", 365, BigDecimal("1200.00")),
            TimeDepositEntity("student", 366, BigDecimal("1200.00")),
            TimeDepositEntity("premium", 45, BigDecimal("1200.00")),
            TimeDepositEntity("premium", 46, BigDecimal("1200.00"))
        )
    private fun demoWithdraws(deposits: List<TimeDepositEntity>) =
        listOf(
            WithdrawalEntity(
                timeDepositId = deposits[1].id!!,
                amount = BigDecimal("50.00"),
                date = LocalDate.of(2026, 9, 1)
            ),
            WithdrawalEntity(
                timeDepositId = deposits[2].id!!,
                amount = BigDecimal("75.50"),
                date = LocalDate.of(2026, 9, 2)
            ),
            WithdrawalEntity(
                timeDepositId = deposits[5].id!!,
                amount = BigDecimal("100.00"),
                date = LocalDate.of(2026, 9, 3)
            )
        )

    companion object {
        private val logger = LoggerFactory.getLogger(TimeDepositDataSeeder::class.java)
    }
}
