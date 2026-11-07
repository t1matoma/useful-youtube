package dev.satoshi.usefulyoutube.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.satoshi.usefulyoutube.core.domain.usecase.ClearHistoryUseCase
import dev.satoshi.usefulyoutube.core.domain.usecase.ObserveWatchHistoryUseCase
import dev.satoshi.usefulyoutube.core.domain.usecase.RemoveFromHistoryUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.ZoneId
import javax.inject.Inject
import kotlin.time.Clock

@HiltViewModel
class HistoryViewModel
    @Inject
    constructor(
        observeWatchHistory: ObserveWatchHistoryUseCase,
        private val removeFromHistory: RemoveFromHistoryUseCase,
        private val clearHistory: ClearHistoryUseCase,
        private val clock: Clock,
        private val zone: ZoneId,
    ) : ViewModel() {
        private val isClearDialogVisible = MutableStateFlow(false)

        val uiState: StateFlow<HistoryUiState> =
            combine(observeWatchHistory(), isClearDialogVisible) { history, dialogVisible ->
                HistoryUiState(
                    isLoading = false,
                    days = groupByDay(history, clock.now(), zone),
                    isClearDialogVisible = dialogVisible,
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), HistoryUiState())

        fun onEvent(event: HistoryUiEvent) {
            when (event) {
                is HistoryUiEvent.VideoRemoved -> {
                    viewModelScope.launch { removeFromHistory(event.videoId) }
                }

                HistoryUiEvent.ClearClicked -> {
                    isClearDialogVisible.value = true
                }

                HistoryUiEvent.ClearConfirmed -> {
                    isClearDialogVisible.value = false
                    viewModelScope.launch { clearHistory() }
                }

                HistoryUiEvent.ClearDismissed -> {
                    isClearDialogVisible.value = false
                }
            }
        }

        private companion object {
            const val STOP_TIMEOUT_MILLIS = 5_000L
        }
    }
