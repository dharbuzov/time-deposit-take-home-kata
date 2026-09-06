package org.ikigaidigital.application.service

import org.ikigaidigital.adapter.`in`.rest.CorrelationIdFilter
import org.ikigaidigital.application.observability.OperationTimer
import org.ikigaidigital.application.port.`in`.UpdateTimeDepositBalancesUseCase
import org.ikigaidigital.application.port.`in`.UpdateTimeDepositBalancesResult
import org.ikigaidigital.application.port.out.TimeDepositPersistencePort
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.stereotype.Service
import java.time.Clock
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorCompletionService
import java.util.concurrent.Executors

/**
 * Coordinates production-style bulk balance updates without one full-table transaction.
 *
 * The coordinator captures one calendar period and one `upperBoundId` at the start, then traverses deposit IDs with
 * keyset batches (`id > lastId`, `id <= upperBoundId`) so memory use does not grow with table size and rows inserted
 * after the run starts are left for the next invocation. Only immutable IDs are passed to workers; JPA entities are
 * loaded inside each worker transaction. Worker submission is bounded to the configured worker count instead of using
 * `parallelStream`, the common pool, or an unbounded queue. The captured period is only an idempotency key; the worker
 * still uses `days` to decide business eligibility before any claim is attempted.
 */
@Service
class UpdateTimeDepositBalancesService(
    private val timeDepositPersistencePort: TimeDepositPersistencePort,
    private val batchProcessor: TimeDepositBalanceBatchProcessor,
    private val periodProvider: TimeDepositPeriodProvider,
    private val clock: Clock,
    private val properties: TimeDepositUpdateProperties
) : UpdateTimeDepositBalancesUseCase {
    override fun updateTimeDepositBalances(): UpdateTimeDepositBalancesResult {
        val timer = OperationTimer.start()
        val period = periodProvider.currentPeriod().toString()
        val upperBoundId = timeDepositPersistencePort.findMaxTimeDepositId()
        val correlationId = MDC.get(CorrelationIdFilter.MDC_KEY)
        var aggregate = TimeDepositBalanceBatchResult.EMPTY

        logger.info(
            "operation=update_balances period={} batchSize={} workers={} upperBoundId={} status=started",
            period,
            properties.batchSize,
            properties.workers,
            upperBoundId
        )

        if (upperBoundId == null) {
            val result = aggregate.toUseCaseResult(period)
            logger.info(
                "operation=update_balances period={} processed={} updated={} alreadyProcessed={} notEligible={} durationMs={} status=success",
                result.period,
                result.processed,
                result.updated,
                result.alreadyProcessed,
                result.notEligible,
                timer.elapsedMs()
            )
            return result
        }

        val executor = Executors.newFixedThreadPool(properties.workers)
        val completionService = ExecutorCompletionService<TimeDepositBalanceBatchResult>(executor)

        return try {
            var lastId = 0
            var batchNumber = 0
            var inFlight = 0
            var exhausted = false

            fun submitNextBatch(): Boolean {
                if (exhausted) {
                    return false
                }

                val ids = timeDepositPersistencePort.findNextTimeDepositIds(
                    lastId = lastId,
                    upperBoundId = upperBoundId,
                    limit = properties.batchSize
                )

                if (ids.isEmpty()) {
                    exhausted = true
                    return false
                }

                lastId = ids.last()
                batchNumber++
                completionService.submit(batchTask(batchNumber, ids, period, correlationId))
                inFlight++
                return true
            }

            repeat(properties.workers) {
                submitNextBatch()
            }

            while (inFlight > 0) {
                val batchResult = try {
                    completionService.take().get()
                } catch (ex: ExecutionException) {
                    throw IllegalStateException("Time deposit balance batch failed", ex.cause ?: ex)
                }
                inFlight--
                aggregate = aggregate.plusCounts(batchResult)
                submitNextBatch()
            }

            val result = aggregate.toUseCaseResult(period)
            logger.info(
                "operation=update_balances period={} processed={} updated={} alreadyProcessed={} notEligible={} durationMs={} status=success",
                result.period,
                result.processed,
                result.updated,
                result.alreadyProcessed,
                result.notEligible,
                timer.elapsedMs()
            )
            result
        } catch (ex: RuntimeException) {
            logger.error(
                "operation=update_balances period={} processed={} updated={} alreadyProcessed={} notEligible={} durationMs={} status=failure errorType={}",
                period,
                aggregate.processed,
                aggregate.updated,
                aggregate.alreadyProcessed,
                aggregate.notEligible,
                timer.elapsedMs(),
                ex.javaClass.simpleName,
                ex
            )
            throw ex
        } finally {
            executor.shutdownNow()
        }
    }

    private fun batchTask(
        batchNumber: Int,
        ids: List<Int>,
        period: String,
        correlationId: String?
    ): Callable<TimeDepositBalanceBatchResult> =
        Callable {
            try {
                if (correlationId != null) {
                    MDC.put(CorrelationIdFilter.MDC_KEY, correlationId)
                }

                batchProcessor.processBatch(
                    batchNumber = batchNumber,
                    timeDepositIds = ids,
                    accrualPeriod = period,
                    claimedAt = clock.instant()
                )
            } finally {
                MDC.remove(CorrelationIdFilter.MDC_KEY)
            }
        }

    private fun TimeDepositBalanceBatchResult.toUseCaseResult(period: String): UpdateTimeDepositBalancesResult =
        UpdateTimeDepositBalancesResult(
            period = period,
            processed = processed,
            updated = updated,
            alreadyProcessed = alreadyProcessed,
            notEligible = notEligible
        )

    companion object {
        private val logger = LoggerFactory.getLogger(UpdateTimeDepositBalancesService::class.java)
    }
}
