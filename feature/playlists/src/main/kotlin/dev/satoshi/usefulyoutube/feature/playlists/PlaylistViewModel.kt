package dev.satoshi.usefulyoutube.feature.playlists

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.satoshi.usefulyoutube.core.domain.usecase.DeletePlaylistUseCase
import dev.satoshi.usefulyoutube.core.domain.usecase.ObservePlaylistUseCase
import dev.satoshi.usefulyoutube.core.domain.usecase.RenamePlaylistUseCase
import dev.satoshi.usefulyoutube.core.domain.usecase.SetVideoInPlaylistUseCase
import dev.satoshi.usefulyoutube.core.model.PlaylistId
import dev.satoshi.usefulyoutube.core.model.PlaylistSource
import dev.satoshi.usefulyoutube.feature.playlists.navigation.PlaylistDestination
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        observePlaylist: ObservePlaylistUseCase,
        private val renamePlaylist: RenamePlaylistUseCase,
        private val deletePlaylist: DeletePlaylistUseCase,
        private val setVideoInPlaylist: SetVideoInPlaylistUseCase,
    ) : ViewModel() {
        private val playlistId =
            PlaylistId(checkNotNull(savedStateHandle[PlaylistDestination.PLAYLIST_ID_ARGUMENT]))
        private val dialogState = MutableStateFlow<PlaylistDialog?>(null)

        val uiState: StateFlow<PlaylistUiState> =
            combine(observePlaylist(playlistId), dialogState) { playlist, dialog ->
                if (playlist == null) PlaylistUiState.NotFound else PlaylistUiState.Content(playlist, dialog)
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), PlaylistUiState.Loading)

        fun onEvent(event: PlaylistUiEvent) {
            val content = uiState.value as? PlaylistUiState.Content ?: return
            // Mirrored YouTube playlists are read-only.
            if (content.playlist.playlist.source != PlaylistSource.LOCAL) return
            when (event) {
                PlaylistUiEvent.RenameClicked -> {
                    dialogState.value = PlaylistDialog.Rename(content.playlist.playlist.title)
                }

                is PlaylistUiEvent.TitleChanged -> {
                    dialogState.update { dialog -> (dialog as? PlaylistDialog.Rename)?.copy(title = event.title) }
                }

                PlaylistUiEvent.RenameConfirmed -> {
                    val title = (dialogState.value as? PlaylistDialog.Rename)?.title ?: return
                    dialogState.value = null
                    viewModelScope.launch { renamePlaylist(playlistId, title) }
                }

                PlaylistUiEvent.DeleteClicked -> {
                    dialogState.value = PlaylistDialog.ConfirmDelete
                }

                PlaylistUiEvent.DeleteConfirmed -> {
                    dialogState.value = null
                    viewModelScope.launch { deletePlaylist(playlistId) }
                }

                PlaylistUiEvent.DialogDismissed -> {
                    dialogState.value = null
                }

                is PlaylistUiEvent.VideoRemoved -> {
                    val video = content.playlist.videos.find { it.id == event.videoId } ?: return
                    viewModelScope.launch { setVideoInPlaylist(playlistId, video, inPlaylist = false) }
                }
            }
        }

        private companion object {
            const val STOP_TIMEOUT_MILLIS = 5_000L
        }
    }
