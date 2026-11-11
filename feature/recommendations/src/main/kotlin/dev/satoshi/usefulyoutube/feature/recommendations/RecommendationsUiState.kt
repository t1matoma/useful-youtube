package dev.satoshi.usefulyoutube.feature.recommendations

import dev.satoshi.usefulyoutube.core.model.DataError
import dev.satoshi.usefulyoutube.core.model.Video

sealed interface RecommendationsUiState {
    data object Loading : RecommendationsUiState

    /** There is nothing to recommend yet, for example before anything was watched or imported. */
    data class Empty(
        val isRefreshing: Boolean,
    ) : RecommendationsUiState

    /** Refresh failed and there is nothing stored to show. */
    data class Error(
        val error: DataError,
        val isRefreshing: Boolean,
    ) : RecommendationsUiState

    /** Videos the user has not watched, the most interesting first. */
    data class Content(
        val videos: List<Video>,
        val isRefreshing: Boolean,
        /** Error of the last refresh, shown once over the stored videos. */
        val refreshError: DataError?,
    ) : RecommendationsUiState
}
