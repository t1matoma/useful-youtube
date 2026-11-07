package dev.satoshi.usefulyoutube.feature.player

import dev.satoshi.usefulyoutube.core.model.ChannelError
import dev.satoshi.usefulyoutube.core.model.Video

sealed interface PlayerUiState {
    data object Loading : PlayerUiState

    data object NotFound : PlayerUiState

    data class Content(
        val video: Video,
        val isDescriptionExpanded: Boolean,
        /** `false` for videos opened from the search, whose channel can be added to the subscriptions. */
        val isChannelSubscribed: Boolean,
        val isSubscribing: Boolean = false,
        val subscribeError: ChannelError? = null,
        /** Set when a playlist is played; `null` for a single video. */
        val queue: PlaybackQueue? = null,
    ) : PlayerUiState
}

/** [position] is 1-based, as shown to the user ("3 of 12"). */
data class PlaybackQueue(
    val playlistTitle: String,
    val position: Int,
    val size: Int,
) {
    val hasPrevious: Boolean get() = position > 1
    val hasNext: Boolean get() = position < size
}
