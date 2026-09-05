package org.ikigaidigital.application.service

import org.ikigaidigital.application.observability.OperationTimer
import org.ikigaidigital.application.port.`in`.GetTimeDepositsUseCase
import org.ikigaidigital.application.port.out.TimeDepositPersistencePort
import org.ikigaidigital.domain.TimeDepositAccount
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GetTimeDepositsService(
    private val timeDepositPersistencePort: TimeDepositPersistencePort
) : GetTimeDepositsUseCase {
    @Transactional(readOnly = true)
    override fun getTimeDeposits(): List<TimeDepositAccount> {
        val timer = OperationTimer.start()

        return try {
            val deposits = timeDepositPersistencePort.findAllWithWithdrawals()

            logger.info(
                "operation=get_time_deposits deposits={} durationMs={} status=success",
                deposits.size,
                timer.elapsedMs()
            )

            deposits
        } catch (ex: RuntimeException) {
            logger.error(
                "operation=get_time_deposits durationMs={} status=failure errorType={}",
                timer.elapsedMs(),
                ex.javaClass.simpleName,
                ex
            )
            throw ex
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(GetTimeDepositsService::class.java)
    }
}
