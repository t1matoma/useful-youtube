package dev.satoshi.usefulyoutube.feature.feed

import dev.satoshi.usefulyoutube.core.model.DataError
import dev.satoshi.usefulyoutube.core.model.PlaybackPosition
import dev.satoshi.usefulyoutube.core.model.Video
import dev.satoshi.usefulyoutube.core.model.VideoId

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

    /** Every stored video was watched to the end, so none is left to show. */
    data class AllWatched(
        val isRefreshing: Boolean,
        /** Error of the last refresh, shown once. */
        val refreshError: DataError?,
    ) : FeedUiState

    /** The videos that were not watched to the end. */
    data class Content(
        val videos: List<Video>,
        val isRefreshing: Boolean,
        /** Error of the last refresh, shown once over the stored videos. */
        val refreshError: DataError?,
        /** Where watched videos stopped, for their progress bars. */
        val playbackPositions: Map<VideoId, PlaybackPosition> = emptyMap(),
    ) : FeedUiState
}
