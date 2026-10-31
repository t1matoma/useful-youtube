package dev.satoshi.usefulyoutube.core.network.model

import kotlinx.serialization.Serializable

/** Response of the Data API `videos.list` with `part=contentDetails,snippet`. */
@Serializable
data class VideoListResponseDto(
    val items: List<VideoItemDto> = emptyList(),
)

@Serializable
data class VideoItemDto(
    val id: String,
    val snippet: VideoSnippetDto,
    val contentDetails: VideoContentDetailsDto? = null,
)

@Serializable
data class VideoSnippetDto(
    val channelId: String,
    val channelTitle: String = "",
    val title: String,
    val description: String = "",
    val publishedAt: String,
    val thumbnails: Map<String, ThumbnailDto> = emptyMap(),
    val liveBroadcastContent: String = "none",
)

@Serializable
data class ThumbnailDto(
    val url: String,
)

@Serializable
data class VideoContentDetailsDto(
    /** ISO 8601 duration, for example `PT4M13S`; `P0D` for live streams and upcoming premieres. */
    val duration: String? = null,
)
