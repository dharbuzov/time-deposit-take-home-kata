package org.ikigaidigital.application.port.`in`

data class TimeDepositPageRequest(
    val page: Int,
    val size: Int,
    val sort: SortSpec
)

data class SortSpec(
    val field: String,
    val direction: SortDirection
)

enum class SortDirection {
    ASC,
    DESC
}

data class PageResult<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
)
