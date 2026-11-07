package dev.satoshi.usefulyoutube.core.network.model

import kotlinx.serialization.Serializable

/** One page of the Data API `playlists.list` with `mine=true&part=snippet`. */
@Serializable
data class PlaylistListResponseDto(
    val items: List<PlaylistDto> = emptyList(),
    val nextPageToken: String? = null,
)

@Serializable
data class PlaylistDto(
    val id: String,
    val snippet: PlaylistSnippetDto,
)

@Serializable
data class PlaylistSnippetDto(
    val title: String,
)

/** One page of the Data API `playlistItems.list` with `part=contentDetails`. */
@Serializable
data class PlaylistItemListResponseDto(
    val items: List<PlaylistItemDto> = emptyList(),
    val nextPageToken: String? = null,
)

@Serializable
data class PlaylistItemDto(
    val contentDetails: PlaylistItemContentDetailsDto,
)

@Serializable
data class PlaylistItemContentDetailsDto(
    val videoId: String,
)
