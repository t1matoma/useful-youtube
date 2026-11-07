package dev.satoshi.usefulyoutube.core.network.model

import kotlinx.serialization.Serializable

/** One page of the Data API `search.list` with `type=video`; details come from `videos.list`. */
@Serializable
data class SearchListResponseDto(
    val items: List<SearchItemDto> = emptyList(),
    val nextPageToken: String? = null,
)

@Serializable
data class SearchItemDto(
    val id: SearchItemIdDto,
)

@Serializable
data class SearchItemIdDto(
    val videoId: String? = null,
)
