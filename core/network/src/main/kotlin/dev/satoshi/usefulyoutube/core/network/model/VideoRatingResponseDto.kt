package dev.satoshi.usefulyoutube.core.network.model

import kotlinx.serialization.Serializable

/** Response of the Data API `videos.getRating`. */
@Serializable
data class VideoRatingListResponseDto(
    val items: List<VideoRatingDto> = emptyList(),
)

@Serializable
data class VideoRatingDto(
    val videoId: String,
    /** `like`, `dislike`, `none` or `unspecified`. */
    val rating: String,
)
