package org.ikigaidigital.adapter.out.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDate

@Entity
@Table(name = "withdrawals")
open class WithdrawalEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    open var id: Int? = null

    @Column(name = "timeDepositId", nullable = false)
    open var timeDepositId: Int = 0

    @Column(name = "amount", nullable = false)
    open var amount: BigDecimal = BigDecimal.ZERO

    @Column(name = "date", nullable = false)
    open var date: LocalDate = LocalDate.MIN

    constructor(
        timeDepositId: Int,
        amount: BigDecimal,
        date: LocalDate
    ) : this() {
        this.id = id
        this.timeDepositId = timeDepositId
        this.amount = amount
        this.date = date
    }
}
