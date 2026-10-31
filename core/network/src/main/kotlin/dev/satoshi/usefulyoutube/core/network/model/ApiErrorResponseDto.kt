package dev.satoshi.usefulyoutube.core.network.model

import kotlinx.serialization.Serializable

/** Error body returned by Google APIs. */
@Serializable
data class ApiErrorResponseDto(
    val error: ApiErrorDto,
)

@Serializable
data class ApiErrorDto(
    val code: Int,
    val errors: List<ApiErrorReasonDto> = emptyList(),
    val details: List<ApiErrorReasonDto> = emptyList(),
)

@Serializable
data class ApiErrorReasonDto(
    val reason: String = "",
)
