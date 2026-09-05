package org.ikigaidigital.application.service

import org.ikigaidigital.application.port.`in`.UpdateTimeDepositBalancesUseCase
import org.ikigaidigital.application.port.out.TimeDepositPersistencePort
import org.ikigaidigital.domain.TimeDepositCalculator
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UpdateTimeDepositBalancesService(
    private val timeDepositPersistencePort: TimeDepositPersistencePort,
    private val timeDepositCalculator: TimeDepositCalculator
) : UpdateTimeDepositBalancesUseCase {
    @Transactional
    override fun updateTimeDepositBalances() {
        val deposits = timeDepositPersistencePort.findAllWithWithdrawals()
            .map(LegacyTimeDepositBalanceMapper::toLegacyTimeDeposit)

        timeDepositCalculator.updateBalance(deposits)

        timeDepositPersistencePort.replaceBalances(
            deposits.map(LegacyTimeDepositBalanceMapper::toTimeDepositBalance)
        )
    }
}
