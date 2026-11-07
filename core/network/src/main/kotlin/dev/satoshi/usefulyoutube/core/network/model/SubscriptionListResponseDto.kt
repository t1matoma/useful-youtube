package dev.satoshi.usefulyoutube.core.network.model

import kotlinx.serialization.Serializable

/** One page of the Data API `subscriptions.list` with `mine=true&part=snippet`. */
@Serializable
data class SubscriptionListResponseDto(
    val items: List<SubscriptionItemDto> = emptyList(),
    val nextPageToken: String? = null,
)

@Serializable
data class SubscriptionItemDto(
    val snippet: SubscriptionSnippetDto,
)

@Serializable
data class SubscriptionSnippetDto(
    val title: String,
    val resourceId: SubscriptionResourceIdDto,
)

@Serializable
data class SubscriptionResourceIdDto(
    val channelId: String,
)
