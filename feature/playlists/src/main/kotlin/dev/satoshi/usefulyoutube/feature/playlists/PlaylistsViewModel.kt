package dev.satoshi.usefulyoutube.feature.playlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.satoshi.usefulyoutube.core.domain.usecase.CreatePlaylistUseCase
import dev.satoshi.usefulyoutube.core.domain.usecase.ObservePlaylistsUseCase
import dev.satoshi.usefulyoutube.core.model.PlaylistSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistsViewModel
    @Inject
    constructor(
        observePlaylists: ObservePlaylistsUseCase,
        private val createPlaylist: CreatePlaylistUseCase,
    ) : ViewModel() {
        private val dialogState = MutableStateFlow<String?>(null)

        val uiState: StateFlow<PlaylistsUiState> =
            combine(observePlaylists(), dialogState) { playlists, newPlaylistTitle ->
                PlaylistsUiState(
                    isLoading = false,
                    localPlaylists = playlists.filter { it.source == PlaylistSource.LOCAL },
                    newPlaylistTitle = newPlaylistTitle,
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), PlaylistsUiState())

        fun onEvent(event: PlaylistsUiEvent) {
            when (event) {
                PlaylistsUiEvent.CreateClicked -> {
                    dialogState.value = ""
                }

                is PlaylistsUiEvent.NewTitleChanged -> {
                    dialogState.update { current -> current?.let { event.title } }
                }

                PlaylistsUiEvent.CreateConfirmed -> {
                    create()
                }

                PlaylistsUiEvent.CreateDismissed -> {
                    dialogState.value = null
                }
            }
        }

        private fun create() {
            val title = dialogState.value?.takeIf { it.isNotBlank() } ?: return
            dialogState.value = null
            viewModelScope.launch { createPlaylist(title) }
        }

        private companion object {
            const val STOP_TIMEOUT_MILLIS = 5_000L
        }
    }
