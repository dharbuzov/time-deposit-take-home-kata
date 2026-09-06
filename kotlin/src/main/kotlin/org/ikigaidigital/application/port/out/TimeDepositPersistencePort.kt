package org.ikigaidigital.application.port.out

import org.ikigaidigital.domain.TimeDepositAccount
import org.ikigaidigital.domain.TimeDepositBalance
import org.ikigaidigital.application.port.`in`.PageResult
import org.ikigaidigital.application.port.`in`.TimeDepositPageRequest
import java.time.Instant

/**
 * Port for persistence operations on time deposits.
 */
interface TimeDepositPersistencePort {

    /**
     * Returns all existing time deposits with their associated withdrawals.
     */
    fun findAllWithWithdrawals(): List<TimeDepositAccount>

    /**
     * Returns one page of existing time deposits with their associated withdrawals.
     */
    fun findPageWithWithdrawals(request: TimeDepositPageRequest): PageResult<TimeDepositAccount>

    /**
     * Captures the stable upper bound for one update run.
     */
    fun findMaxTimeDepositId(): Int?

    /**
     * Returns the next bounded keyset page of IDs. This keeps update-all traversal independent from JPA entities and
     * avoids `OFFSET` work that grows with table size.
     */
    fun findNextTimeDepositIds(lastId: Int, upperBoundId: Int, limit: Int): List<Int>

    /**
     * Loads deposits inside the worker transaction from immutable IDs supplied by the coordinator.
     */
    fun findByIds(timeDepositIds: List<Int>): List<TimeDepositAccount>

    /**
     * Attempts the monthly idempotency claim atomically. Implementations must use one insert guarded by the database
     * unique constraint, not check-then-insert application logic.
     */
    fun tryClaimMonthlyInterest(timeDepositId: Int, accrualPeriod: String, createdAt: Instant): Boolean

    /**
     * Replaces the balances of all time deposits with the given balances.
     */
    fun replaceBalances(balances: List<TimeDepositBalance>)
}
