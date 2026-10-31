package dev.satoshi.usefulyoutube.core.network.model

import kotlinx.serialization.Serializable

/** Response of the Data API `channels.list` with `part=snippet`. */
@Serializable
data class ChannelListResponseDto(
    val items: List<ChannelItemDto> = emptyList(),
)

@Serializable
data class ChannelItemDto(
    val id: String,
    val snippet: ChannelSnippetDto,
)

@Serializable
data class ChannelSnippetDto(
    val title: String,
)
