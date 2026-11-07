package dev.satoshi.usefulyoutube.feature.playlists

import dev.satoshi.usefulyoutube.core.model.VideoId

sealed interface PlaylistUiEvent {
    data object RenameClicked : PlaylistUiEvent

    data class TitleChanged(
        val title: String,
    ) : PlaylistUiEvent

    data object RenameConfirmed : PlaylistUiEvent

    data object DeleteClicked : PlaylistUiEvent

    /** The screen navigates back right after sending it. */
    data object DeleteConfirmed : PlaylistUiEvent

    data object DialogDismissed : PlaylistUiEvent

    data class VideoRemoved(
        val videoId: VideoId,
    ) : PlaylistUiEvent
}
