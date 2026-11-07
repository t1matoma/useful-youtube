package dev.satoshi.usefulyoutube.feature.history

import dev.satoshi.usefulyoutube.core.model.VideoId

sealed interface HistoryUiEvent {
    data class VideoRemoved(
        val videoId: VideoId,
    ) : HistoryUiEvent

    data object ClearClicked : HistoryUiEvent

    data object ClearConfirmed : HistoryUiEvent

    data object ClearDismissed : HistoryUiEvent
}
