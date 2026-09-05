package org.ikigaidigital.application.service

import org.ikigaidigital.domain.TimeDeposit
import org.ikigaidigital.domain.TimeDepositAccount
import org.ikigaidigital.domain.TimeDepositBalance
import java.math.BigDecimal

object LegacyTimeDepositBalanceMapper {
    fun toLegacyTimeDeposit(timeDeposit: TimeDepositAccount): TimeDeposit =
        TimeDeposit(
            id = timeDeposit.id,
            planType = timeDeposit.planType,
            balance = timeDeposit.balance.toDouble(),
            days = timeDeposit.days
        )

    fun toTimeDepositBalance(timeDeposit: TimeDeposit): TimeDepositBalance =
        TimeDepositBalance(
            timeDepositId = timeDeposit.id,
            balance = BigDecimal.valueOf(timeDeposit.balance)
        )
}
