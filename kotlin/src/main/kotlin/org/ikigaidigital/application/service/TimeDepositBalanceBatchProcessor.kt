package org.ikigaidigital.application.service

import org.ikigaidigital.application.observability.OperationTimer
import org.ikigaidigital.application.port.out.TimeDepositPersistencePort
import org.ikigaidigital.domain.TimeDepositCalculator
import org.ikigaidigital.domain.interest.InterestPolicyResolver
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * Processes one bounded batch in its own Spring transaction.
 *
 * This bean is intentionally separate from the coordinator so `@Transactional` is applied through a Spring proxy
 * instead of being bypassed by self-invocation. A batch transaction contains the monthly claim, legacy calculation,
 * and balance update together. If the batch fails, both inserted claims and balance changes roll back, so the failed
 * IDs can be safely retried while previously committed batches remain committed.
 */
@Component
class TimeDepositBalanceBatchProcessor(
    private val timeDepositPersistencePort: TimeDepositPersistencePort,
    private val timeDepositCalculator: TimeDepositCalculator,
    private val interestPolicyResolver: InterestPolicyResolver
) {
    @Transactional
    fun processBatch(
        batchNumber: Int,
        timeDepositIds: List<Int>,
        accrualPeriod: String,
        claimedAt: Instant
    ): TimeDepositBalanceBatchResult {
        val timer = OperationTimer.start()
        val orderById = timeDepositIds.withIndex().associate { it.value to it.index }
        val deposits = timeDepositPersistencePort.findByIds(timeDepositIds)
            .sortedBy { orderById[it.id] ?: Int.MAX_VALUE }

        var alreadyProcessed = 0
        var notEligible = 0

        val claimedDeposits = deposits.mapNotNull { deposit ->
            if (!interestPolicyResolver.isEligible(deposit.planType, deposit.days)) {
                notEligible++
                null
            } else if (!timeDepositPersistencePort.tryClaimMonthlyInterest(deposit.id, accrualPeriod, claimedAt)) {
                alreadyProcessed++
                null
            } else {
                LegacyTimeDepositBalanceMapper.toLegacyTimeDeposit(deposit)
            }
        }

        timeDepositCalculator.updateBalance(claimedDeposits)
        timeDepositPersistencePort.replaceBalances(
            claimedDeposits.map(LegacyTimeDepositBalanceMapper::toTimeDepositBalance)
        )

        val result = TimeDepositBalanceBatchResult(
            batchNumber = batchNumber,
            processed = deposits.size,
            updated = claimedDeposits.size,
            alreadyProcessed = alreadyProcessed,
            notEligible = notEligible
        )

        logger.debug(
            "operation=update_balance_batch batch={} size={} updated={} alreadyProcessed={} notEligible={} durationMs={}",
            batchNumber,
            deposits.size,
            result.updated,
            result.alreadyProcessed,
            result.notEligible,
            timer.elapsedMs()
        )

        return result
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TimeDepositBalanceBatchProcessor::class.java)
    }
}
