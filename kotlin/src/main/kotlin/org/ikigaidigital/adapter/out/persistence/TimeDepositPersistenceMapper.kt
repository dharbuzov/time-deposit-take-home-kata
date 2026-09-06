package org.ikigaidigital.adapter.out.persistence

import org.ikigaidigital.domain.TimeDepositAccount
import org.ikigaidigital.domain.Withdrawal

object TimeDepositPersistenceMapper {
    fun toDomain(entity: TimeDepositEntity): TimeDepositAccount =
        toDomain(entity, entity.withdrawals)

    fun toDomainWithoutWithdrawals(entity: TimeDepositEntity): TimeDepositAccount =
        toDomain(entity, emptyList())

    fun toDomain(entity: TimeDepositEntity, withdrawals: List<WithdrawalEntity>): TimeDepositAccount =
        TimeDepositAccount(
            id = entity.id!!,
            planType = entity.planType,
            balance = entity.balance,
            days = entity.days,
            withdrawals = withdrawals.map(::toDomain)
        )

    private fun toDomain(entity: WithdrawalEntity): Withdrawal =
        Withdrawal(
            id = entity.id!!,
            amount = entity.amount,
            date = entity.date
        )
}
