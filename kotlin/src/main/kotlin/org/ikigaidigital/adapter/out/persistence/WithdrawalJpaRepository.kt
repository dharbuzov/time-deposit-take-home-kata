package org.ikigaidigital.adapter.out.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface WithdrawalJpaRepository : JpaRepository<WithdrawalEntity, Int>
