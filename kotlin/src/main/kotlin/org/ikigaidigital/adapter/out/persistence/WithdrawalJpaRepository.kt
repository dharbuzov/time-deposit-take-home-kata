package org.ikigaidigital.adapter.out.persistence

import org.springframework.data.jpa.repository.JpaRepository

/**
 * JPA repository for [WithdrawalEntity]
 */
interface WithdrawalJpaRepository : JpaRepository<WithdrawalEntity, Int>
