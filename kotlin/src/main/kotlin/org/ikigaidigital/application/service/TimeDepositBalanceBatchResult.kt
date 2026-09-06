package org.ikigaidigital.application.service

data class TimeDepositBalanceBatchResult(
    val batchNumber: Int,
    val processed: Int,
    val updated: Int,
    val alreadyProcessed: Int,
    val notEligible: Int
) {
    init {
        require(processed == updated + alreadyProcessed + notEligible) {
            "processed must equal updated + alreadyProcessed + notEligible"
        }
    }

    fun plusCounts(other: TimeDepositBalanceBatchResult): TimeDepositBalanceBatchResult =
        TimeDepositBalanceBatchResult(
            batchNumber = other.batchNumber,
            processed = processed + other.processed,
            updated = updated + other.updated,
            alreadyProcessed = alreadyProcessed + other.alreadyProcessed,
            notEligible = notEligible + other.notEligible
        )

    companion object {
        val EMPTY = TimeDepositBalanceBatchResult(
            batchNumber = 0,
            processed = 0,
            updated = 0,
            alreadyProcessed = 0,
            notEligible = 0
        )
    }
}
