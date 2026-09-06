package org.ikigaidigital.adapter.out.persistence

import org.ikigaidigital.application.port.`in`.PageResult
import org.ikigaidigital.application.port.`in`.SortDirection
import org.ikigaidigital.application.port.`in`.TimeDepositPageRequest
import org.ikigaidigital.application.port.out.TimeDepositPersistencePort
import org.ikigaidigital.domain.TimeDepositAccount
import org.ikigaidigital.domain.TimeDepositBalance
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component
import java.sql.Timestamp
import java.time.Instant

/**
 * Adapter for persistence operations on time deposits.
 *
 * The monthly accrual claim uses PostgreSQL `ON CONFLICT DO NOTHING` against
 * `UNIQUE(time_deposit_id, accrual_period)`. That unique constraint is the cross-thread and cross-instance
 * concurrency invariant; a check-then-insert would race and could double-apply monthly interest.
 */
@Component
class TimeDepositPersistenceAdapter(
    private val timeDepositRepository: TimeDepositJpaRepository,
    private val withdrawalRepository: WithdrawalJpaRepository,
    private val jdbcTemplate: JdbcTemplate
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

    override fun findMaxTimeDepositId(): Int? =
        jdbcTemplate.queryForObject(
            "SELECT MAX(id) FROM \"timeDeposits\"",
            Int::class.java
        )

    override fun findNextTimeDepositIds(lastId: Int, upperBoundId: Int, limit: Int): List<Int> =
        jdbcTemplate.queryForList(
            """
            SELECT id
            FROM "timeDeposits"
            WHERE id > ?
              AND id <= ?
            ORDER BY id
            LIMIT ?
            """.trimIndent(),
            Int::class.java,
            lastId,
            upperBoundId,
            limit
        )

    override fun findByIds(timeDepositIds: List<Int>): List<TimeDepositAccount> =
        timeDepositRepository.findAllById(timeDepositIds)
            .map(TimeDepositPersistenceMapper::toDomainWithoutWithdrawals)

    override fun tryClaimMonthlyInterest(timeDepositId: Int, accrualPeriod: String, createdAt: Instant): Boolean {
        val insertedRows = jdbcTemplate.update(
            """
            INSERT INTO time_deposit_interest_accruals(time_deposit_id, accrual_period, created_at)
            VALUES (?, ?, ?)
            ON CONFLICT (time_deposit_id, accrual_period) DO NOTHING
            """.trimIndent(),
            timeDepositId,
            accrualPeriod,
            Timestamp.from(createdAt)
        )

        return insertedRows == 1
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
