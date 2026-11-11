package dev.satoshi.usefulyoutube.core.network.model

import kotlinx.serialization.Serializable

/** Response of the Data API `channelSections.list` with `part=snippet,contentDetails`. */
@Serializable
data class ChannelSectionListResponseDto(
    val items: List<ChannelSectionDto> = emptyList(),
)

@Serializable
data class ChannelSectionDto(
    val snippet: ChannelSectionSnippetDto,
    val contentDetails: ChannelSectionContentDetailsDto? = null,
)

@Serializable
data class ChannelSectionSnippetDto(
    /** For example `multiplechannels`, the section with channels the author features. */
    val type: String = "",
)

@Serializable
data class ChannelSectionContentDetailsDto(
    val channels: List<String> = emptyList(),
)
