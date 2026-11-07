package dev.satoshi.usefulyoutube.feature.playlists

import dev.satoshi.usefulyoutube.core.model.Playlist

data class PlaylistsUiState(
    val isLoading: Boolean = true,
    val localPlaylists: List<Playlist> = emptyList(),
    /** The title in the "new playlist" dialog; `null` while the dialog is hidden. */
    val newPlaylistTitle: String? = null,
)
