package dev.satoshi.usefulyoutube.feature.recommendations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.satoshi.usefulyoutube.core.domain.usecase.ObserveRecommendationsUseCase
import dev.satoshi.usefulyoutube.core.domain.usecase.RefreshRecommendationsUseCase
import dev.satoshi.usefulyoutube.core.model.AppResult
import dev.satoshi.usefulyoutube.core.model.DataError
import dev.satoshi.usefulyoutube.core.model.Video
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Videos outside the subscriptions are looked for once when the tab opens and on pull-to-refresh. */
@HiltViewModel
class RecommendationsViewModel
    @Inject
    constructor(
        observeRecommendations: ObserveRecommendationsUseCase,
        private val refreshRecommendations: RefreshRecommendationsUseCase,
    ) : ViewModel() {
        private val refreshState = MutableStateFlow(RefreshState())

        val uiState: StateFlow<RecommendationsUiState> =
            combine(observeRecommendations(), refreshState, ::toUiState)
                .stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                    RecommendationsUiState.Loading,
                )

        init {
            refresh()
        }

        fun onEvent(event: RecommendationsUiEvent) {
            when (event) {
                RecommendationsUiEvent.Refresh -> refresh()
                RecommendationsUiEvent.RefreshErrorShown -> refreshState.update { it.copy(error = null) }
            }
        }

        private fun refresh() {
            if (refreshState.value.isRefreshing) return
            refreshState.value = RefreshState(isRefreshing = true)
            viewModelScope.launch {
                val error = (refreshRecommendations() as? AppResult.Failure)?.error
                refreshState.value = RefreshState(isRefreshing = false, error = error)
            }
        }

        private fun toUiState(
            videos: List<Video>,
            refresh: RefreshState,
        ): RecommendationsUiState =
            when {
                videos.isNotEmpty() -> RecommendationsUiState.Content(videos, refresh.isRefreshing, refresh.error)
                refresh.error != null -> RecommendationsUiState.Error(refresh.error, refresh.isRefreshing)
                else -> RecommendationsUiState.Empty(refresh.isRefreshing)
            }

        private data class RefreshState(
            val isRefreshing: Boolean = false,
            val error: DataError? = null,
        )

        private companion object {
            const val STOP_TIMEOUT_MILLIS = 5_000L
        }
    }
