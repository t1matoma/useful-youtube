package dev.satoshi.usefulyoutube.feature.player.rating

import dev.satoshi.usefulyoutube.core.model.VideoId

sealed interface RatingUiEvent {
    /** The player switched to another video of a playlist. */
    data class VideoChanged(
        val videoId: VideoId,
    ) : RatingUiEvent

    /** Likes the video, or removes the like when it is already liked. */
    data object LikeClicked : RatingUiEvent

    /** Dislikes the video, or removes the dislike when it is already disliked. */
    data object DislikeClicked : RatingUiEvent

    data object ConsentLaunched : RatingUiEvent

    data class ConsentFinished(
        val granted: Boolean,
    ) : RatingUiEvent
}
