package dev.satoshi.usefulyoutube.core.domain.repository

import dev.satoshi.usefulyoutube.core.model.AppResult
import dev.satoshi.usefulyoutube.core.model.DataError
import dev.satoshi.usefulyoutube.core.model.SearchOrder
import dev.satoshi.usefulyoutube.core.model.SearchResults
import dev.satoshi.usefulyoutube.core.model.Video
import dev.satoshi.usefulyoutube.core.model.VideoId
import kotlinx.coroutines.flow.Flow

/** Videos found on YouTube with loaded details; their kinds are not determined yet. */
data class SearchPage(
    val videos: List<Video>,
    val nextPageToken: String?,
)

/** The stored search; [nextPageToken] is `null` when there are no more pages. */
data class SearchSession(
    val query: String,
    val order: SearchOrder,
    val nextPageToken: String?,
)

interface SearchRepository {
    /** The last search with its stored results, or `null` before the first search. */
    fun observeResults(): Flow<SearchResults?>

    fun observeVideo(id: VideoId): Flow<Video?>

    suspend fun getSession(): SearchSession?

    /** Searches YouTube; [pageToken] is `null` for the first page. */
    suspend fun fetchPage(
        query: String,
        order: SearchOrder,
        pageToken: String?,
    ): AppResult<SearchPage, DataError>

    /** Replaces the stored search with the first page of a new one. */
    suspend fun saveFirstPage(
        session: SearchSession,
        videos: List<Video>,
    )

    /** Appends the next page; videos that are already stored are skipped. */
    suspend fun saveNextPage(
        nextPageToken: String?,
        videos: List<Video>,
    )

    /** Most recent first. */
    fun observeRecentQueries(): Flow<List<String>>

    suspend fun saveRecentQuery(query: String)

    suspend fun deleteRecentQuery(query: String)
}
