package dev.satoshi.usefulyoutube.feature.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.satoshi.usefulyoutube.core.domain.usecase.ObserveVideoUseCase
import dev.satoshi.usefulyoutube.core.model.VideoId
import dev.satoshi.usefulyoutube.feature.player.navigation.PlayerDestination
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        observeVideo: ObserveVideoUseCase,
    ) : ViewModel() {
        private val videoId = VideoId(checkNotNull(savedStateHandle[PlayerDestination.VIDEO_ID_ARGUMENT]))
        private val isDescriptionExpanded = MutableStateFlow(false)

        val uiState: StateFlow<PlayerUiState> =
            combine(observeVideo(videoId), isDescriptionExpanded) { video, expanded ->
                if (video == null) PlayerUiState.NotFound else PlayerUiState.Content(video, expanded)
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), PlayerUiState.Loading)

        fun onEvent(event: PlayerUiEvent) {
            when (event) {
                PlayerUiEvent.DescriptionToggled -> isDescriptionExpanded.update { !it }
            }
        }

        private companion object {
            const val STOP_TIMEOUT_MILLIS = 5_000L
        }
    }
