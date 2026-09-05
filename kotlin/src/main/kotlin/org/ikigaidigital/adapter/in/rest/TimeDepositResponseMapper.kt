package org.ikigaidigital.adapter.`in`.rest

import org.ikigaidigital.application.port.`in`.PageResult
import org.ikigaidigital.domain.TimeDepositAccount
import org.ikigaidigital.domain.Withdrawal

/**
 * Maps domain objects to REST responses.
 */
object TimeDepositResponseMapper {
    fun toPageResponse(page: PageResult<TimeDepositAccount>): TimeDepositPageResponse =
        TimeDepositPageResponse(
            content = page.content.map(::toResponse),
            page = page.page,
            size = page.size,
            totalElements = page.totalElements,
            totalPages = page.totalPages
        )

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
