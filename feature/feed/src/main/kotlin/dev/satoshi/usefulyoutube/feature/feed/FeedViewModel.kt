package dev.satoshi.usefulyoutube.feature.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.satoshi.usefulyoutube.core.domain.usecase.ObserveFeedUseCase
import dev.satoshi.usefulyoutube.core.domain.usecase.RefreshFeedUseCase
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

@HiltViewModel
class FeedViewModel
    @Inject
    constructor(
        observeFeed: ObserveFeedUseCase,
        private val refreshFeed: RefreshFeedUseCase,
    ) : ViewModel() {
        private val refreshState = MutableStateFlow(RefreshState())

        val uiState: StateFlow<FeedUiState> =
            combine(observeFeed(), refreshState, ::toUiState)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), FeedUiState.Loading)

        init {
            refresh()
        }

        fun onEvent(event: FeedUiEvent) {
            when (event) {
                FeedUiEvent.Refresh -> refresh()
                FeedUiEvent.RefreshErrorShown -> refreshState.update { it.copy(error = null) }
            }
        }

        private fun refresh() {
            if (refreshState.value.isRefreshing) return
            refreshState.value = RefreshState(isRefreshing = true)
            viewModelScope.launch {
                val error = (refreshFeed() as? AppResult.Failure)?.error
                refreshState.value = RefreshState(isRefreshing = false, error = error)
            }
        }

        private fun toUiState(
            videos: List<Video>,
            refresh: RefreshState,
        ): FeedUiState =
            when {
                videos.isNotEmpty() -> FeedUiState.Content(videos, refresh.isRefreshing, refresh.error)
                refresh.error != null -> FeedUiState.Error(refresh.error, refresh.isRefreshing)
                else -> FeedUiState.Empty(refresh.isRefreshing)
            }

        private data class RefreshState(
            val isRefreshing: Boolean = false,
            val error: DataError? = null,
        )

        private companion object {
            const val STOP_TIMEOUT_MILLIS = 5_000L
        }
    }
