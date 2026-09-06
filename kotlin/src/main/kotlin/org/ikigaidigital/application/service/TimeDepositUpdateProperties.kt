package org.ikigaidigital.application.service

import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * Runtime tuning for the bulk balance update use case.
 *
 * `batchSize` limits how many deposit IDs a worker processes in one transaction: larger values reduce coordinator
 * overhead, while smaller values reduce rollback scope and memory held per batch. `workers` bounds database
 * concurrency; the default of four is deliberately below Spring Boot/Hikari's default maximum pool size of ten so
 * request handling and other database work keep connection headroom.
 */
@Component
class TimeDepositUpdateProperties(
    @Value("\${app.time-deposit.update.batch-size:500}")
    val batchSize: Int,
    @Value("\${app.time-deposit.update.workers:4}")
    val workers: Int
) {
    @PostConstruct
    fun validate() {
        require(batchSize > 0) { "app.time-deposit.update.batch-size must be positive" }
        require(workers > 0) { "app.time-deposit.update.workers must be positive" }
    }
}
