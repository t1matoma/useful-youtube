package dev.satoshi.usefulyoutube.feature.playlists

import dev.satoshi.usefulyoutube.core.model.PlaylistWithVideos

sealed interface PlaylistUiState {
    data object Loading : PlaylistUiState

    data object NotFound : PlaylistUiState

    data class Content(
        val playlist: PlaylistWithVideos,
        val dialog: PlaylistDialog? = null,
    ) : PlaylistUiState
}

sealed interface PlaylistDialog {
    data class Rename(
        val title: String,
    ) : PlaylistDialog

    data object ConfirmDelete : PlaylistDialog
}
