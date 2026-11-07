package dev.satoshi.usefulyoutube.core.model

enum class SearchOrder {
    RELEVANCE,
    DATE,
}

/** The last search on YouTube and the results loaded for it so far, without Shorts. */
data class SearchResults(
    val query: String,
    val order: SearchOrder,
    val videos: List<Video>,
    val canLoadMore: Boolean,
)
