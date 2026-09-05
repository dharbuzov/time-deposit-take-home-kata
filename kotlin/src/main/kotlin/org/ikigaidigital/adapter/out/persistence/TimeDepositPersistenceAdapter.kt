package org.ikigaidigital.adapter.out.persistence

import org.ikigaidigital.application.port.`in`.PageResult
import org.ikigaidigital.application.port.`in`.SortDirection
import org.ikigaidigital.application.port.`in`.TimeDepositPageRequest
import org.ikigaidigital.application.port.out.TimeDepositPersistencePort
import org.ikigaidigital.domain.TimeDepositAccount
import org.ikigaidigital.domain.TimeDepositBalance
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component

/**
 * Adapter for persistence operations on time deposits.
 */
@Component
class TimeDepositPersistenceAdapter(
    private val timeDepositRepository: TimeDepositJpaRepository,
    private val withdrawalRepository: WithdrawalJpaRepository
) : TimeDepositPersistencePort {

    override fun findAllWithWithdrawals(): List<TimeDepositAccount> =
        timeDepositRepository.findAllByOrderByIdAsc()
            .map(TimeDepositPersistenceMapper::toDomain)

    override fun findPageWithWithdrawals(request: TimeDepositPageRequest): PageResult<TimeDepositAccount> {
        val page = timeDepositRepository.findAll(
            PageRequest.of(
                request.page,
                request.size,
                Sort.by(toSpringDirection(request.sort.direction), request.sort.field)
            )
        )

        val depositIds = page.content.mapNotNull { it.id }
        val withdrawalsByDepositId = if (depositIds.isEmpty()) {
            emptyMap()
        } else {
            withdrawalRepository.findByTimeDepositIdInOrderByTimeDepositIdAscIdAsc(depositIds)
                .groupBy { it.timeDepositId }
        }

        return PageResult(
            content = page.content.map { deposit ->
                TimeDepositPersistenceMapper.toDomain(
                    deposit,
                    withdrawalsByDepositId[deposit.id] ?: emptyList()
                )
            },
            page = request.page,
            size = request.size,
            totalElements = page.totalElements,
            totalPages = page.totalPages
        )
    }

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

    private fun toSpringDirection(direction: SortDirection): Sort.Direction =
        when (direction) {
            SortDirection.ASC -> Sort.Direction.ASC
            SortDirection.DESC -> Sort.Direction.DESC
        }
}
