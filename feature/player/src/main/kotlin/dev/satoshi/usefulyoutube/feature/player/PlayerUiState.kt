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
    ) : PlayerUiState
}
