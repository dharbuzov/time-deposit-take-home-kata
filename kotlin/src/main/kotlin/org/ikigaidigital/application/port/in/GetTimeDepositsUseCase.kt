package org.ikigaidigital.application.port.`in`

import org.ikigaidigital.domain.TimeDepositAccount

/**
 * Use case for getting all time deposits.
 */
interface GetTimeDepositsUseCase {

    /**
     * Returns all existing time deposits.
     */
    fun getTimeDeposits(): List<TimeDepositAccount>
}
