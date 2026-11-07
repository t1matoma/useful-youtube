package dev.satoshi.usefulyoutube.feature.player.save

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.satoshi.usefulyoutube.core.domain.usecase.CreatePlaylistUseCase
import dev.satoshi.usefulyoutube.core.domain.usecase.ObservePlaylistsContainingVideoUseCase
import dev.satoshi.usefulyoutube.core.domain.usecase.ObservePlaylistsUseCase
import dev.satoshi.usefulyoutube.core.domain.usecase.ObserveVideoUseCase
import dev.satoshi.usefulyoutube.core.domain.usecase.SetVideoInPlaylistUseCase
import dev.satoshi.usefulyoutube.core.model.PlaylistSource
import dev.satoshi.usefulyoutube.core.model.Video
import dev.satoshi.usefulyoutube.core.model.VideoId
import dev.satoshi.usefulyoutube.feature.player.navigation.PlayerDestination
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** State of the "Save to playlist" sheet of the player, kept apart from the playback state. */
@HiltViewModel
class SaveToPlaylistViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        observePlaylists: ObservePlaylistsUseCase,
        observePlaylistsContainingVideo: ObservePlaylistsContainingVideoUseCase,
        private val observeVideo: ObserveVideoUseCase,
        private val setVideoInPlaylist: SetVideoInPlaylistUseCase,
        private val createPlaylist: CreatePlaylistUseCase,
    ) : ViewModel() {
        private data class SheetState(
            val isSheetVisible: Boolean = false,
            val newPlaylistTitle: String? = null,
        )

        private val videoId = VideoId(checkNotNull(savedStateHandle[PlayerDestination.VIDEO_ID_ARGUMENT]))
        private val sheetState = MutableStateFlow(SheetState())

        val uiState: StateFlow<SaveToPlaylistUiState> =
            combine(
                observePlaylists(),
                observePlaylistsContainingVideo(videoId),
                sheetState,
            ) { playlists, containing, sheet ->
                SaveToPlaylistUiState(
                    isSheetVisible = sheet.isSheetVisible,
                    playlists =
                        playlists
                            .filter { it.source == PlaylistSource.LOCAL }
                            .map { PlaylistOption(it.id, it.title, containsVideo = it.id in containing) },
                    newPlaylistTitle = sheet.newPlaylistTitle,
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), SaveToPlaylistUiState())

        fun onEvent(event: SaveToPlaylistUiEvent) {
            when (event) {
                SaveToPlaylistUiEvent.SaveClicked -> {
                    sheetState.update { it.copy(isSheetVisible = true) }
                }

                SaveToPlaylistUiEvent.SheetDismissed -> {
                    sheetState.update { it.copy(isSheetVisible = false) }
                }

                is SaveToPlaylistUiEvent.PlaylistToggled -> {
                    withVideo { video -> setVideoInPlaylist(event.id, video, event.containsVideo) }
                }

                SaveToPlaylistUiEvent.NewPlaylistClicked -> {
                    sheetState.update { it.copy(newPlaylistTitle = "") }
                }

                is SaveToPlaylistUiEvent.NewPlaylistTitleChanged -> {
                    sheetState.update { state ->
                        state.copy(newPlaylistTitle = state.newPlaylistTitle?.let { event.title })
                    }
                }

                SaveToPlaylistUiEvent.NewPlaylistConfirmed -> {
                    createWithVideo()
                }

                SaveToPlaylistUiEvent.NewPlaylistDismissed -> {
                    sheetState.update { it.copy(newPlaylistTitle = null) }
                }
            }
        }

        private fun createWithVideo() {
            val title = sheetState.value.newPlaylistTitle?.takeIf { it.isNotBlank() } ?: return
            sheetState.update { it.copy(newPlaylistTitle = null) }
            withVideo { video -> createPlaylist(title, firstVideo = video) }
        }

        private fun withVideo(action: suspend (Video) -> Unit) {
            viewModelScope.launch { observeVideo(videoId).first()?.let { action(it) } }
        }

        private companion object {
            const val STOP_TIMEOUT_MILLIS = 5_000L
        }
    }
