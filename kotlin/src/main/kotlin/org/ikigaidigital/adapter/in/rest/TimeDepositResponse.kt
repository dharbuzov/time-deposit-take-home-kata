package org.ikigaidigital.adapter.`in`.rest

import java.math.BigDecimal
import java.time.LocalDate

data class TimeDepositPageResponse(
    val content: List<TimeDepositResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
)

data class TimeDepositResponse(
    val id: Int,
    val planType: String,
    val balance: BigDecimal,
    val days: Int,
    val withdrawals: List<WithdrawalResponse>
)

data class WithdrawalResponse(
    val id: Int,
    val amount: BigDecimal,
    val date: LocalDate
)

/**
 * API summary for POST /time-deposits/balances.
 *
 * `processed` is every deposit considered by workers. `updated` is every eligible deposit whose monthly claim was
 * acquired and whose balance update completed. `alreadyProcessed` is every eligible deposit already claimed for the
 * period. `notEligible` is every deposit rejected by the `days`-based business rules.
 */
data class UpdateTimeDepositBalancesResponse(
    val period: String,
    val processed: Int,
    val updated: Int,
    val alreadyProcessed: Int,
    val notEligible: Int
)
