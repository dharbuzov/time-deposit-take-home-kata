package org.ikigaidigital.adapter.`in`.rest

import org.ikigaidigital.domain.TimeDepositAccount
import org.ikigaidigital.domain.Withdrawal

object TimeDepositResponseMapper {
    fun toResponse(timeDeposit: TimeDepositAccount): TimeDepositResponse =
        TimeDepositResponse(
            id = timeDeposit.id,
            planType = timeDeposit.planType,
            balance = timeDeposit.balance,
            days = timeDeposit.days,
            withdrawals = timeDeposit.withdrawals.map(::toResponse)
        )

    private fun toResponse(withdrawal: Withdrawal): WithdrawalResponse =
        WithdrawalResponse(
            id = withdrawal.id,
            amount = withdrawal.amount,
            date = withdrawal.date
        )
}
