package org.ikigaidigital.application.service

import org.ikigaidigital.application.port.`in`.GetTimeDepositsUseCase
import org.ikigaidigital.application.port.out.TimeDepositPersistencePort
import org.ikigaidigital.domain.TimeDepositAccount
import org.springframework.stereotype.Service

@Service
class GetTimeDepositsService(
    private val timeDepositPersistencePort: TimeDepositPersistencePort
) : GetTimeDepositsUseCase {
    override fun getTimeDeposits(): List<TimeDepositAccount> =
        timeDepositPersistencePort.findAllWithWithdrawals()
}
