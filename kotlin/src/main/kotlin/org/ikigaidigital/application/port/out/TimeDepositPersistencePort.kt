package org.ikigaidigital.application.port.out

import org.ikigaidigital.domain.TimeDepositAccount
import org.ikigaidigital.domain.TimeDepositBalance

interface TimeDepositPersistencePort {
    fun findAllWithWithdrawals(): List<TimeDepositAccount>

    fun replaceBalances(balances: List<TimeDepositBalance>)
}
