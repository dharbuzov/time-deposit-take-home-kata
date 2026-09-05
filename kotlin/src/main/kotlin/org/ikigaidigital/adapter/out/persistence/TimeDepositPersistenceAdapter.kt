package org.ikigaidigital.adapter.out.persistence

import org.ikigaidigital.application.port.out.TimeDepositPersistencePort
import org.ikigaidigital.domain.TimeDepositAccount
import org.ikigaidigital.domain.TimeDepositBalance
import org.springframework.stereotype.Component

@Component
class TimeDepositPersistenceAdapter : TimeDepositPersistencePort {
    override fun findAllWithWithdrawals(): List<TimeDepositAccount> {
        throw UnsupportedOperationException("Time deposit persistence is not implemented yet.")
    }

    override fun replaceBalances(balances: List<TimeDepositBalance>) {
        throw UnsupportedOperationException("Time deposit balance persistence is not implemented yet.")
    }
}
