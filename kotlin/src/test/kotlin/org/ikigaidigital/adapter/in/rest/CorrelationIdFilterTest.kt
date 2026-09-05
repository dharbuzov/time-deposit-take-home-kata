package org.ikigaidigital.adapter.`in`.rest

import org.assertj.core.api.Assertions.assertThat
import org.ikigaidigital.application.port.`in`.GetTimeDepositsUseCase
import org.ikigaidigital.application.port.`in`.PageResult
import org.ikigaidigital.application.port.`in`.SortDirection
import org.ikigaidigital.application.port.`in`.SortSpec
import org.ikigaidigital.application.port.`in`.TimeDepositPageRequest
import org.ikigaidigital.application.port.`in`.UpdateTimeDepositBalancesUseCase
import org.ikigaidigital.domain.TimeDepositAccount
import org.junit.jupiter.api.Test
import org.mockito.Mockito.doAnswer
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

@WebMvcTest(TimeDepositController::class)
@Import(CorrelationIdFilter::class)
class CorrelationIdFilterTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var getTimeDepositsUseCase: GetTimeDepositsUseCase

    @MockBean
    private lateinit var updateTimeDepositBalancesUseCase: UpdateTimeDepositBalancesUseCase

    @Test
    fun `returns incoming correlation id and exposes it through MDC during request handling`() {
        doAnswer {
            assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isEqualTo("test-correlation-id")
            emptyPage()
        }.`when`(getTimeDepositsUseCase).getTimeDeposits(defaultRequest())

        mockMvc.perform(
            get("/time-deposits")
                .header(CorrelationIdFilter.CORRELATION_ID_HEADER, "test-correlation-id")
        )
            .andExpect(status().isOk)
            .andExpect(header().string(CorrelationIdFilter.CORRELATION_ID_HEADER, "test-correlation-id"))

        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull()
    }

    @Test
    fun `generates correlation id when request header is missing`() {
        doAnswer {
            val correlationId = MDC.get(CorrelationIdFilter.MDC_KEY)
            assertThat(correlationId).isNotBlank()
            UUID.fromString(correlationId)
            emptyPage()
        }.`when`(getTimeDepositsUseCase).getTimeDeposits(defaultRequest())

        mockMvc.perform(get("/time-deposits"))
            .andExpect(status().isOk)
            .andExpect(header().exists(CorrelationIdFilter.CORRELATION_ID_HEADER))

        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull()
    }

    @Test
    fun `generates correlation id when request header is blank`() {
        doAnswer {
            val correlationId = MDC.get(CorrelationIdFilter.MDC_KEY)
            assertThat(correlationId).isNotBlank()
            assertThat(correlationId).isNotEqualTo(" ")
            UUID.fromString(correlationId)
            emptyPage()
        }.`when`(getTimeDepositsUseCase).getTimeDeposits(defaultRequest())

        mockMvc.perform(
            get("/time-deposits")
                .header(CorrelationIdFilter.CORRELATION_ID_HEADER, " ")
        )
            .andExpect(status().isOk)
            .andExpect(header().exists(CorrelationIdFilter.CORRELATION_ID_HEADER))

        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull()
    }

    @Test
    fun `clears MDC after failed request processing`() {
        doAnswer {
            assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isEqualTo("failing-request")
            throw IllegalStateException("Simulated failure")
        }.`when`(updateTimeDepositBalancesUseCase).updateTimeDepositBalances()

        try {
            mockMvc.perform(
                post("/time-deposits/balances")
                    .header(CorrelationIdFilter.CORRELATION_ID_HEADER, "failing-request")
            )
        } catch (ex: Exception) {
            assertThat(ex).hasCauseInstanceOf(IllegalStateException::class.java)
        }

        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull()
    }

    private fun defaultRequest(): TimeDepositPageRequest =
        TimeDepositPageRequest(
            page = 0,
            size = 20,
            sort = SortSpec("id", SortDirection.ASC)
        )

    private fun emptyPage(): PageResult<TimeDepositAccount> =
        PageResult(
            content = emptyList(),
            page = 0,
            size = 20,
            totalElements = 0,
            totalPages = 0
        )
}
