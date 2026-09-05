package org.ikigaidigital.application.port.out

import org.ikigaidigital.domain.TimeDepositAccount
import org.ikigaidigital.domain.TimeDepositBalance

/**
 * Port for persistence operations on time deposits.
 */
interface TimeDepositPersistencePort {

    /**
     * Returns all existing time deposits with their associated withdrawals.
     */
    fun findAllWithWithdrawals(): List<TimeDepositAccount>

    /**
     * Replaces the balances of all time deposits with the given balances.
     */
    fun replaceBalances(balances: List<TimeDepositBalance>)
}
