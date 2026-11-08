package dev.satoshi.usefulyoutube.feature.playlists

import dev.satoshi.usefulyoutube.core.model.PlaybackPosition
import dev.satoshi.usefulyoutube.core.model.PlaylistWithVideos
import dev.satoshi.usefulyoutube.core.model.VideoId

sealed interface PlaylistUiState {
    data object Loading : PlaylistUiState

    data object NotFound : PlaylistUiState

    data class Content(
        val playlist: PlaylistWithVideos,
        val dialog: PlaylistDialog? = null,
        /** Where watched videos stopped, for their progress bars. */
        val playbackPositions: Map<VideoId, PlaybackPosition> = emptyMap(),
    ) : PlaylistUiState
}

sealed interface PlaylistDialog {
    data class Rename(
        val title: String,
    ) : PlaylistDialog

    data object ConfirmDelete : PlaylistDialog
}
