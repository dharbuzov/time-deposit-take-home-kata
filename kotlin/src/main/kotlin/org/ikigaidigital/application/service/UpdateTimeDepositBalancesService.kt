package org.ikigaidigital.application.service

import org.ikigaidigital.application.port.`in`.UpdateTimeDepositBalancesUseCase
import org.ikigaidigital.application.port.out.TimeDepositPersistencePort
import org.springframework.stereotype.Service

@Service
class UpdateTimeDepositBalancesService(
    private val timeDepositPersistencePort: TimeDepositPersistencePort
) : UpdateTimeDepositBalancesUseCase {
    override fun updateTimeDepositBalances() {
        throw UnsupportedOperationException("Balance update orchestration is not implemented yet.")
    }
}
