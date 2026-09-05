package org.ikigaidigital.application.service

import org.ikigaidigital.application.port.`in`.GetTimeDepositsUseCase
import org.ikigaidigital.application.port.out.TimeDepositPersistencePort
import org.ikigaidigital.domain.TimeDepositAccount
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GetTimeDepositsService(
    private val timeDepositPersistencePort: TimeDepositPersistencePort
) : GetTimeDepositsUseCase {
    @Transactional(readOnly = true)
    override fun getTimeDeposits(): List<TimeDepositAccount> =
        timeDepositPersistencePort.findAllWithWithdrawals()
}
