package org.ikigaidigital.application.port.`in`

import org.ikigaidigital.domain.TimeDepositAccount

/**
 * Use case for getting time deposits.
 */
interface GetTimeDepositsUseCase {

    /**
     * Returns one page of existing time deposits.
     */
    fun getTimeDeposits(request: TimeDepositPageRequest): PageResult<TimeDepositAccount>
}
