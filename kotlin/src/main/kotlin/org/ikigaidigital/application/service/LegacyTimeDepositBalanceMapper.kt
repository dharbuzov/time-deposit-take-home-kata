package org.ikigaidigital.application.service

import org.ikigaidigital.domain.TimeDeposit
import org.ikigaidigital.domain.TimeDepositAccount
import org.ikigaidigital.domain.TimeDepositBalance
import org.ikigaidigital.domain.Money

object LegacyTimeDepositBalanceMapper {
    fun toLegacyTimeDeposit(timeDeposit: TimeDepositAccount): TimeDeposit =
        TimeDeposit(
            id = timeDeposit.id,
            planType = timeDeposit.planType,
            balance = Money.persistedAmount(timeDeposit.balance).toLegacyDouble(),
            days = timeDeposit.days
        )

    fun toTimeDepositBalance(timeDeposit: TimeDeposit): TimeDepositBalance =
        TimeDepositBalance(
            timeDepositId = timeDeposit.id,
            balance = Money.persistedAmount(timeDeposit.balance).amount
        )
}
