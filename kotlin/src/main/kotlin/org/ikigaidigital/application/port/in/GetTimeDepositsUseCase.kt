package org.ikigaidigital.application.port.`in`

import org.ikigaidigital.domain.TimeDepositAccount

interface GetTimeDepositsUseCase {
    fun getTimeDeposits(): List<TimeDepositAccount>
}
