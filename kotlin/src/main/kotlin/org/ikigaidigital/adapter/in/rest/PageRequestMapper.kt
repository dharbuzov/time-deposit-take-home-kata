package org.ikigaidigital.adapter.`in`.rest

import org.ikigaidigital.application.port.`in`.SortDirection
import org.ikigaidigital.application.port.`in`.SortSpec
import org.ikigaidigital.application.port.`in`.TimeDepositPageRequest
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

object PageRequestMapper {
    const val DEFAULT_PAGE = "0"
    const val DEFAULT_SIZE = "20"
    const val DEFAULT_SORT = "id,asc"
    const val MAX_PAGE_SIZE = 100

    fun toPageRequest(page: Int, size: Int, sort: String): TimeDepositPageRequest {
        if (page < 0) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "page must be greater than or equal to 0")
        }
        if (size < 1) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "size must be greater than or equal to 1")
        }
        if (size > MAX_PAGE_SIZE) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "size must be less than or equal to $MAX_PAGE_SIZE")
        }

        return TimeDepositPageRequest(
            page = page,
            size = size,
            sort = parseSort(sort)
        )
    }

    private fun parseSort(sort: String): SortSpec {
        val parts = sort.split(",")
        if (parts.size != 2 || parts[0] !in SUPPORTED_SORT_FIELDS) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "sort must use a supported field and direction")
        }

        val direction = when (parts[1].lowercase()) {
            "asc" -> SortDirection.ASC
            "desc" -> SortDirection.DESC
            else -> throw ResponseStatusException(HttpStatus.BAD_REQUEST, "sort direction must be asc or desc")
        }

        return SortSpec(field = parts[0], direction = direction)
    }

    private val SUPPORTED_SORT_FIELDS = setOf("id")
}
