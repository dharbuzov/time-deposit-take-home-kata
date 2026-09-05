package org.ikigaidigital.adapter.`in`.rest

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.ikigaidigital.application.port.`in`.SortDirection
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

class PageRequestMapperTest {
    @Test
    fun `maps valid query parameters to application page request`() {
        val request = PageRequestMapper.toPageRequest(1, 2, "id,desc")

        assertThat(request.page).isEqualTo(1)
        assertThat(request.size).isEqualTo(2)
        assertThat(request.sort.field).isEqualTo("id")
        assertThat(request.sort.direction).isEqualTo(SortDirection.DESC)
    }

    @Test
    fun `rejects invalid query parameters with bad request status`() {
        assertBadRequest { PageRequestMapper.toPageRequest(-1, 20, "id,asc") }
        assertBadRequest { PageRequestMapper.toPageRequest(0, 0, "id,asc") }
        assertBadRequest { PageRequestMapper.toPageRequest(0, 101, "id,asc") }
        assertBadRequest { PageRequestMapper.toPageRequest(0, 20, "balance,asc") }
        assertBadRequest { PageRequestMapper.toPageRequest(0, 20, "id,sideways") }
    }

    private fun assertBadRequest(action: () -> Unit) {
        assertThatThrownBy(action)
            .isInstanceOf(ResponseStatusException::class.java)
            .extracting { (it as ResponseStatusException).statusCode }
            .isEqualTo(HttpStatus.BAD_REQUEST)
    }
}
