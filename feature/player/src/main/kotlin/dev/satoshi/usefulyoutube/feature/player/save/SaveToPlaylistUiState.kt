package dev.satoshi.usefulyoutube.feature.player.save

import dev.satoshi.usefulyoutube.core.model.PlaylistId

data class SaveToPlaylistUiState(
    val isSheetVisible: Boolean = false,
    /** App playlists; mirrored YouTube playlists are read-only and not offered. */
    val playlists: List<PlaylistOption> = emptyList(),
    /** The title in the "new playlist" dialog; `null` while the dialog is hidden. */
    val newPlaylistTitle: String? = null,
)

data class PlaylistOption(
    val id: PlaylistId,
    val title: String,
    val containsVideo: Boolean,
)
