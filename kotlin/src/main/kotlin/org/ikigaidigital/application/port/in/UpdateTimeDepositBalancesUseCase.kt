package org.ikigaidigital.application.port.`in`

/**
 * Use case for updating the balances of all time deposits.
 */
interface UpdateTimeDepositBalancesUseCase {

    /**
     * Updates the balances of all existing time deposits.
     */
    fun updateTimeDepositBalances()
}
