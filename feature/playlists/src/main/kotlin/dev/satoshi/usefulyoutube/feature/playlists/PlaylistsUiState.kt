package dev.satoshi.usefulyoutube.feature.playlists

import android.content.IntentSender
import dev.satoshi.usefulyoutube.core.model.AccountError
import dev.satoshi.usefulyoutube.core.model.Playlist

data class PlaylistsUiState(
    val isLoading: Boolean = true,
    val localPlaylists: List<Playlist> = emptyList(),
    /** The stored copy of the account's playlists; shown even while the account is not connected. */
    val youtubePlaylists: List<Playlist> = emptyList(),
    val account: AccountState = AccountState.CHECKING,
    val isSyncing: Boolean = false,
    val syncError: AccountError? = null,
    /** The screen launches it and reports back with [PlaylistsUiEvent.ConsentFinished]. */
    val consentScreen: IntentSender? = null,
    /** The title in the "new playlist" dialog; `null` while the dialog is hidden. */
    val newPlaylistTitle: String? = null,
)

enum class AccountState {
    CHECKING,
    CONNECTED,
    NOT_CONNECTED,
}
