package dev.satoshi.usefulyoutube.feature.channels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.satoshi.usefulyoutube.core.domain.usecase.AddChannelUseCase
import dev.satoshi.usefulyoutube.core.domain.usecase.ObserveChannelsUseCase
import dev.satoshi.usefulyoutube.core.domain.usecase.RemoveChannelUseCase
import dev.satoshi.usefulyoutube.core.model.AppResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChannelsViewModel
    @Inject
    constructor(
        observeChannels: ObserveChannelsUseCase,
        private val addChannel: AddChannelUseCase,
        private val removeChannel: RemoveChannelUseCase,
    ) : ViewModel() {
        private val editorState = MutableStateFlow(ChannelsUiState())

        val uiState: StateFlow<ChannelsUiState> =
            combine(observeChannels(), editorState) { channels, editor ->
                editor.copy(isLoading = false, channels = channels)
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ChannelsUiState())

        fun onEvent(event: ChannelsUiEvent) {
            when (event) {
                is ChannelsUiEvent.InputChanged -> {
                    editorState.update { it.copy(input = event.input, addError = null) }
                }

                ChannelsUiEvent.AddClicked -> {
                    add()
                }

                is ChannelsUiEvent.RemoveRequested -> {
                    editorState.update {
                        it.copy(
                            channelPendingRemoval = event.channel,
                        )
                    }
                }

                ChannelsUiEvent.RemoveConfirmed -> {
                    confirmRemoval()
                }

                ChannelsUiEvent.RemoveDismissed -> {
                    editorState.update { it.copy(channelPendingRemoval = null) }
                }
            }
        }

        private fun add() {
            val input = editorState.value.input
            if (editorState.value.isAdding || input.isBlank()) return
            editorState.update { it.copy(isAdding = true, addError = null) }
            viewModelScope.launch {
                val result = addChannel(input)
                editorState.update { state ->
                    when (result) {
                        is AppResult.Success -> state.copy(isAdding = false, input = "")
                        is AppResult.Failure -> state.copy(isAdding = false, addError = result.error)
                    }
                }
            }
        }

        private fun confirmRemoval() {
            val channel = editorState.value.channelPendingRemoval ?: return
            editorState.update { it.copy(channelPendingRemoval = null) }
            viewModelScope.launch { removeChannel(channel.id) }
        }

        private companion object {
            const val STOP_TIMEOUT_MILLIS = 5_000L
        }
    }
