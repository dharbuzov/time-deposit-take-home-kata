package org.ikigaidigital.adapter.`in`.rest

import org.ikigaidigital.application.port.`in`.GetTimeDepositsUseCase
import org.ikigaidigital.application.port.`in`.UpdateTimeDepositBalancesUseCase
import org.ikigaidigital.adapter.`in`.rest.PageRequestMapper.DEFAULT_PAGE
import org.ikigaidigital.adapter.`in`.rest.PageRequestMapper.DEFAULT_SIZE
import org.ikigaidigital.adapter.`in`.rest.PageRequestMapper.DEFAULT_SORT
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/time-deposits")
class TimeDepositController(
    private val getTimeDepositsUseCase: GetTimeDepositsUseCase,
    private val updateTimeDepositBalancesUseCase: UpdateTimeDepositBalancesUseCase
) {
    @GetMapping
    fun getTimeDeposits(
        @RequestParam(defaultValue = DEFAULT_PAGE) page: Int,
        @RequestParam(defaultValue = DEFAULT_SIZE) size: Int,
        @RequestParam(defaultValue = DEFAULT_SORT) sort: String
    ): TimeDepositPageResponse =
        TimeDepositResponseMapper.toPageResponse(
            getTimeDepositsUseCase.getTimeDeposits(PageRequestMapper.toPageRequest(page, size, sort))
        )

    @PostMapping("/balances")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun updateTimeDepositBalances() {
        updateTimeDepositBalancesUseCase.updateTimeDepositBalances()
    }
}
