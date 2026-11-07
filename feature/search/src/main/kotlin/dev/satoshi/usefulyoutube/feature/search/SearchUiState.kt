package dev.satoshi.usefulyoutube.feature.search

import dev.satoshi.usefulyoutube.core.model.DataError
import dev.satoshi.usefulyoutube.core.model.SearchOrder
import dev.satoshi.usefulyoutube.core.model.SearchResults

data class SearchUiState(
    val query: String = "",
    val order: SearchOrder = SearchOrder.RELEVANCE,
    val recentQueries: List<String> = emptyList(),
    /** The stored results of the last search; `null` before the first search. */
    val results: SearchResults? = null,
    val isSearching: Boolean = false,
    val searchError: DataError? = null,
    val isLoadingMore: Boolean = false,
    val loadMoreError: DataError? = null,
)
