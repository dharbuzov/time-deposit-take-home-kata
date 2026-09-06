package org.ikigaidigital.application.service

import org.springframework.stereotype.Component
import java.time.Clock
import java.time.YearMonth

@Component
class TimeDepositPeriodProvider(
    private val clock: Clock
) {
    /**
     * Returns the calendar month used as the idempotency period.
     *
     * The coordinator captures this value once per HTTP request and passes the resulting `YYYY-MM` value to every
     * worker. Tests replace the clock so monthly retry and next-month behavior do not depend on the real date.
     */
    fun currentPeriod(): YearMonth =
        YearMonth.now(clock)
}
