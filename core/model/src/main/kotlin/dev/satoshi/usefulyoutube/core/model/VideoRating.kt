package dev.satoshi.usefulyoutube.core.model

/** The user's rating of a video in their YouTube account. */
enum class VideoRating {
    LIKE,
    DISLIKE,
    NONE,
}

/** A video the user liked or disliked; [rating] is never [VideoRating.NONE]. */
data class RatedVideo(
    val videoId: VideoId,
    val channelId: ChannelId,
    val rating: VideoRating,
)
