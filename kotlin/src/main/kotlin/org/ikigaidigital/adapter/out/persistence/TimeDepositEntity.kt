package org.ikigaidigital.adapter.out.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderBy
import jakarta.persistence.Table
import java.math.BigDecimal

@Entity
@Table(name = "timeDeposits")
open class TimeDepositEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    open var id: Int? = null

    @Column(name = "planType", nullable = false)
    open var planType: String = ""

    @Column(name = "days", nullable = false)
    open var days: Int = 0

    @Column(name = "balance", nullable = false)
    open var balance: BigDecimal = BigDecimal.ZERO

    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "timeDepositId",
        referencedColumnName = "id",
        insertable = false,
        updatable = false
    )
    @OrderBy("id ASC")
    open var withdrawals: MutableList<WithdrawalEntity> = mutableListOf()

    constructor(
        planType: String,
        days: Int,
        balance: BigDecimal
    ) : this() {
        this.id = id
        this.planType = planType
        this.days = days
        this.balance = balance
    }
}
