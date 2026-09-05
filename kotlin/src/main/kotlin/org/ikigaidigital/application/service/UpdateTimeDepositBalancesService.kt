package org.ikigaidigital.application.service

import org.ikigaidigital.application.port.`in`.UpdateTimeDepositBalancesUseCase
import org.ikigaidigital.application.port.out.TimeDepositPersistencePort
import org.ikigaidigital.application.observability.OperationTimer
import org.ikigaidigital.domain.TimeDepositCalculator
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UpdateTimeDepositBalancesService(
    private val timeDepositPersistencePort: TimeDepositPersistencePort,
    private val timeDepositCalculator: TimeDepositCalculator
) : UpdateTimeDepositBalancesUseCase {
    @Transactional
    override fun updateTimeDepositBalances() {
        var processedDeposits = 0
        val timer = OperationTimer.start()

        try {
            val deposits = timeDepositPersistencePort.findAllWithWithdrawals()
                .map(LegacyTimeDepositBalanceMapper::toLegacyTimeDeposit)

            processedDeposits = deposits.size
            timeDepositCalculator.updateBalance(deposits)

            timeDepositPersistencePort.replaceBalances(
                deposits.map(LegacyTimeDepositBalanceMapper::toTimeDepositBalance)
            )

            logger.info(
                "operation=update_balances deposits={} durationMs={} status=success",
                processedDeposits,
                timer.elapsedMs()
            )
        } catch (ex: RuntimeException) {
            logger.error(
                "operation=update_balances deposits={} durationMs={} status=failure errorType={}",
                processedDeposits,
                timer.elapsedMs(),
                ex.javaClass.simpleName,
                ex
            )
            throw ex
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(UpdateTimeDepositBalancesService::class.java)
    }
}
