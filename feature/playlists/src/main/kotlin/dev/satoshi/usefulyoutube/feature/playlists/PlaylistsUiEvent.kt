package dev.satoshi.usefulyoutube.feature.playlists

sealed interface PlaylistsUiEvent {
    data object CreateClicked : PlaylistsUiEvent

    data class NewTitleChanged(
        val title: String,
    ) : PlaylistsUiEvent

    data object CreateConfirmed : PlaylistsUiEvent

    data object CreateDismissed : PlaylistsUiEvent
}
