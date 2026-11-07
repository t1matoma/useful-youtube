package dev.satoshi.usefulyoutube.feature.playlists

sealed interface PlaylistsUiEvent {
    data object CreateClicked : PlaylistsUiEvent

    data class NewTitleChanged(
        val title: String,
    ) : PlaylistsUiEvent

    data object CreateConfirmed : PlaylistsUiEvent

    data object CreateDismissed : PlaylistsUiEvent

    /** Pull-to-refresh: loads the YouTube playlists again. */
    data object Refresh : PlaylistsUiEvent

    data object ConnectAccountClicked : PlaylistsUiEvent

    data object ConsentLaunched : PlaylistsUiEvent

    data class ConsentFinished(
        val granted: Boolean,
    ) : PlaylistsUiEvent
}
