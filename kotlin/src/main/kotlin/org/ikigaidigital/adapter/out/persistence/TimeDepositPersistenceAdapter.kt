package org.ikigaidigital.adapter.out.persistence

import org.ikigaidigital.application.port.out.TimeDepositPersistencePort
import org.ikigaidigital.domain.TimeDepositAccount
import org.ikigaidigital.domain.TimeDepositBalance
import org.springframework.stereotype.Component

/**
 * Adapter for persistence operations on time deposits.
 */
@Component
class TimeDepositPersistenceAdapter(
    private val timeDepositRepository: TimeDepositJpaRepository
) : TimeDepositPersistencePort {

    override fun findAllWithWithdrawals(): List<TimeDepositAccount> =
        timeDepositRepository.findAllByOrderByIdAsc()
            .map(TimeDepositPersistenceMapper::toDomain)

    override fun replaceBalances(balances: List<TimeDepositBalance>) {
        if (balances.isEmpty()) {
            return
        }

        val entitiesById = timeDepositRepository.findAllById(balances.map { it.timeDepositId })
            .associateBy { it.id }

        val updatedEntities = balances.map { balance ->
            val entity = requireNotNull(entitiesById[balance.timeDepositId]) {
                "Time deposit ${balance.timeDepositId} was not found."
            }
            entity.balance = balance.balance
            entity
        }

        timeDepositRepository.saveAll(updatedEntities)
    }
}
