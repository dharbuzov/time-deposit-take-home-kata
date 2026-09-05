package org.ikigaidigital.adapter.`in`.rest

import org.ikigaidigital.application.port.`in`.GetTimeDepositsUseCase
import org.ikigaidigital.application.port.`in`.UpdateTimeDepositBalancesUseCase
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/time-deposits")
class TimeDepositController(
    private val getTimeDepositsUseCase: GetTimeDepositsUseCase,
    private val updateTimeDepositBalancesUseCase: UpdateTimeDepositBalancesUseCase
) {
    @GetMapping
    fun getTimeDeposits(): List<TimeDepositResponse> =
        getTimeDepositsUseCase.getTimeDeposits()
            .map(TimeDepositResponseMapper::toResponse)

    @PostMapping("/balances")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun updateTimeDepositBalances() {
        updateTimeDepositBalancesUseCase.updateTimeDepositBalances()
    }
}
