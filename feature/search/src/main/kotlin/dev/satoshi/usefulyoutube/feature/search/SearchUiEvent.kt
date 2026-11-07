package dev.satoshi.usefulyoutube.feature.search

import dev.satoshi.usefulyoutube.core.model.SearchOrder

sealed interface SearchUiEvent {
    data class QueryChanged(
        val query: String,
    ) : SearchUiEvent

    data object SearchSubmitted : SearchUiEvent

    data class OrderSelected(
        val order: SearchOrder,
    ) : SearchUiEvent

    data class RecentQuerySelected(
        val query: String,
    ) : SearchUiEvent

    data class RecentQueryDeleted(
        val query: String,
    ) : SearchUiEvent

    data object LoadMoreRequested : SearchUiEvent
}
