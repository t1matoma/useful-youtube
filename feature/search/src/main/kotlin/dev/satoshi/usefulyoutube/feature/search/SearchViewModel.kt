package dev.satoshi.usefulyoutube.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.satoshi.usefulyoutube.core.domain.usecase.DeleteRecentSearchQueryUseCase
import dev.satoshi.usefulyoutube.core.domain.usecase.LoadMoreSearchResultsUseCase
import dev.satoshi.usefulyoutube.core.domain.usecase.ObservePlaybackPositionsUseCase
import dev.satoshi.usefulyoutube.core.domain.usecase.ObserveRecentSearchQueriesUseCase
import dev.satoshi.usefulyoutube.core.domain.usecase.ObserveSearchResultsUseCase
import dev.satoshi.usefulyoutube.core.domain.usecase.SearchVideosUseCase
import dev.satoshi.usefulyoutube.core.model.AppResult
import dev.satoshi.usefulyoutube.core.model.DataError
import dev.satoshi.usefulyoutube.core.model.SearchOrder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel
    @Inject
    constructor(
        observeSearchResults: ObserveSearchResultsUseCase,
        observeRecentQueries: ObserveRecentSearchQueriesUseCase,
        observePlaybackPositions: ObservePlaybackPositionsUseCase,
        private val searchVideos: SearchVideosUseCase,
        private val loadMoreResults: LoadMoreSearchResultsUseCase,
        private val deleteRecentQuery: DeleteRecentSearchQueryUseCase,
    ) : ViewModel() {
        /** [query] and [order] are `null` until the user changes them, so the stored search is shown. */
        private data class EditorState(
            val query: String? = null,
            val order: SearchOrder? = null,
            val isSearching: Boolean = false,
            val searchError: DataError? = null,
            val isLoadingMore: Boolean = false,
            val loadMoreError: DataError? = null,
        )

        private val editorState = MutableStateFlow(EditorState())

        val uiState: StateFlow<SearchUiState> =
            combine(
                observeSearchResults(),
                observeRecentQueries(),
                editorState,
                observePlaybackPositions(),
            ) { results, recentQueries, editor, positions ->
                SearchUiState(
                    query = editor.query ?: results?.query.orEmpty(),
                    order = editor.order ?: results?.order ?: SearchOrder.RELEVANCE,
                    recentQueries = recentQueries,
                    results = results,
                    isSearching = editor.isSearching,
                    searchError = editor.searchError,
                    isLoadingMore = editor.isLoadingMore,
                    loadMoreError = editor.loadMoreError,
                    playbackPositions = positions,
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), SearchUiState())

        fun onEvent(event: SearchUiEvent) {
            when (event) {
                is SearchUiEvent.QueryChanged -> {
                    editorState.update { it.copy(query = event.query) }
                }

                SearchUiEvent.SearchSubmitted -> {
                    search(uiState.value.query, uiState.value.order)
                }

                is SearchUiEvent.OrderSelected -> {
                    selectOrder(event.order)
                }

                is SearchUiEvent.RecentQuerySelected -> {
                    editorState.update { it.copy(query = event.query) }
                    search(event.query, uiState.value.order)
                }

                is SearchUiEvent.RecentQueryDeleted -> {
                    viewModelScope.launch { deleteRecentQuery(event.query) }
                }

                SearchUiEvent.LoadMoreRequested -> {
                    loadMore()
                }
            }
        }

        /** Another order is a new search, so it runs right away for the current query. */
        private fun selectOrder(order: SearchOrder) {
            if (order == uiState.value.order) return
            editorState.update { it.copy(order = order) }
            search(uiState.value.query, order)
        }

        private fun search(
            query: String,
            order: SearchOrder,
        ) {
            val editor = editorState.value
            if (query.isBlank() || editor.isSearching || editor.isLoadingMore) return
            editorState.update {
                it.copy(query = query.trim(), isSearching = true, searchError = null, loadMoreError = null)
            }
            viewModelScope.launch {
                val error = (searchVideos(query, order) as? AppResult.Failure)?.error
                editorState.update { it.copy(isSearching = false, searchError = error) }
            }
        }

        private fun loadMore() {
            val editor = editorState.value
            if (editor.isSearching || editor.isLoadingMore || uiState.value.results?.canLoadMore != true) return
            editorState.update { it.copy(isLoadingMore = true, loadMoreError = null) }
            viewModelScope.launch {
                val error = (loadMoreResults() as? AppResult.Failure)?.error
                editorState.update { it.copy(isLoadingMore = false, loadMoreError = error) }
            }
        }

        private companion object {
            const val STOP_TIMEOUT_MILLIS = 5_000L
        }
    }
