package dev.satoshi.usefulyoutube.feature.feed

import dev.satoshi.usefulyoutube.core.model.DataError
import dev.satoshi.usefulyoutube.core.model.Video

sealed interface FeedUiState {
    data object Loading : FeedUiState

    data class Empty(
        val isRefreshing: Boolean,
    ) : FeedUiState

    /** Refresh failed and there is nothing stored to show. */
    data class Error(
        val error: DataError,
        val isRefreshing: Boolean,
    ) : FeedUiState

    data class Content(
        val videos: List<Video>,
        val isRefreshing: Boolean,
        /** Error of the last refresh, shown once over the stored videos. */
        val refreshError: DataError?,
    ) : FeedUiState
}
