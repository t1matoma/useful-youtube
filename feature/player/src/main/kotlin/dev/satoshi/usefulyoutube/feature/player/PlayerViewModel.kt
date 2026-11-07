package dev.satoshi.usefulyoutube.feature.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.satoshi.usefulyoutube.core.domain.usecase.AddChannelUseCase
import dev.satoshi.usefulyoutube.core.domain.usecase.ObserveChannelsUseCase
import dev.satoshi.usefulyoutube.core.domain.usecase.ObserveVideoUseCase
import dev.satoshi.usefulyoutube.core.model.AppResult
import dev.satoshi.usefulyoutube.core.model.ChannelError
import dev.satoshi.usefulyoutube.core.model.VideoId
import dev.satoshi.usefulyoutube.feature.player.navigation.PlayerDestination
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        observeVideo: ObserveVideoUseCase,
        observeChannels: ObserveChannelsUseCase,
        private val addChannel: AddChannelUseCase,
    ) : ViewModel() {
        private data class ScreenState(
            val isDescriptionExpanded: Boolean = false,
            val isSubscribing: Boolean = false,
            val subscribeError: ChannelError? = null,
        )

        private val videoId = VideoId(checkNotNull(savedStateHandle[PlayerDestination.VIDEO_ID_ARGUMENT]))
        private val screenState = MutableStateFlow(ScreenState())

        val uiState: StateFlow<PlayerUiState> =
            combine(observeVideo(videoId), observeChannels(), screenState) { video, channels, screen ->
                if (video == null) {
                    PlayerUiState.NotFound
                } else {
                    PlayerUiState.Content(
                        video = video,
                        isDescriptionExpanded = screen.isDescriptionExpanded,
                        isChannelSubscribed = channels.any { it.id == video.channelId },
                        isSubscribing = screen.isSubscribing,
                        subscribeError = screen.subscribeError,
                    )
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), PlayerUiState.Loading)

        fun onEvent(event: PlayerUiEvent) {
            when (event) {
                PlayerUiEvent.DescriptionToggled -> {
                    screenState.update { it.copy(isDescriptionExpanded = !it.isDescriptionExpanded) }
                }

                PlayerUiEvent.SubscribeToChannel -> {
                    subscribe()
                }
            }
        }

        /** The channel id is verified through its RSS feed, which costs no Data API quota. */
        private fun subscribe() {
            val content = uiState.value as? PlayerUiState.Content ?: return
            if (content.isSubscribing || content.isChannelSubscribed) return
            screenState.update { it.copy(isSubscribing = true, subscribeError = null) }
            viewModelScope.launch {
                val result = addChannel(content.video.channelId.value)
                val error =
                    (result as? AppResult.Failure)?.error?.takeUnless { it == ChannelError.AlreadySubscribed }
                screenState.update { it.copy(isSubscribing = false, subscribeError = error) }
            }
        }

        private companion object {
            const val STOP_TIMEOUT_MILLIS = 5_000L
        }
    }
