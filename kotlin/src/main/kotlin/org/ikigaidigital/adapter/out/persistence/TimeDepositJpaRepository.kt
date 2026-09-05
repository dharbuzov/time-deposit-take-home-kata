package org.ikigaidigital.adapter.out.persistence

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository

interface TimeDepositJpaRepository : JpaRepository<TimeDepositEntity, Int> {
    @EntityGraph(attributePaths = ["withdrawals"])
    fun findAllByOrderByIdAsc(): List<TimeDepositEntity>
}
