package org.ikigaidigital.application.port.`in`

/**
 * Use case for updating the balances of all time deposits.
 */
interface UpdateTimeDepositBalancesUseCase {

    /**
     * Updates eligible balances for the current monthly accrual period.
     */
    fun updateTimeDepositBalances(): UpdateTimeDepositBalancesResult
}

/**
 * Summary returned by the bulk balance update use case.
 *
 * `processed` is every deposit considered by workers. `updated` is every eligible deposit whose monthly claim was
 * acquired and whose balance update completed. `alreadyProcessed` is every eligible deposit that already had a
 * claim for the operation period. `notEligible` is every deposit rejected by the `days`-based business rules.
 */
data class UpdateTimeDepositBalancesResult(
    val period: String,
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
}
