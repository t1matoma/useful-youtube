package dev.satoshi.usefulyoutube.core.testing.repository

import dev.satoshi.usefulyoutube.core.domain.repository.SearchPage
import dev.satoshi.usefulyoutube.core.domain.repository.SearchRepository
import dev.satoshi.usefulyoutube.core.domain.repository.SearchSession
import dev.satoshi.usefulyoutube.core.model.AppResult
import dev.satoshi.usefulyoutube.core.model.DataError
import dev.satoshi.usefulyoutube.core.model.SearchOrder
import dev.satoshi.usefulyoutube.core.model.SearchResults
import dev.satoshi.usefulyoutube.core.model.Video
import dev.satoshi.usefulyoutube.core.model.VideoId
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeSearchRepository(
    initialSession: SearchSession? = null,
    initialResults: List<Video> = emptyList(),
) : SearchRepository {
    private val session = MutableStateFlow(initialSession)
    private val results = MutableStateFlow(initialResults)
    private val recentQueries = MutableStateFlow(emptyList<String>())

    /** Pages returned by [fetchPage], keyed by query and page token (`null` for the first page). */
    val remotePages = mutableMapOf<Pair<String, String?>, SearchPage>()

    /** When set, [fetchPage] fails with this error. */
    var fetchError: DataError? = null

    val fetchedPages = mutableListOf<Triple<String, SearchOrder, String?>>()

    /** Suspends [fetchPage] until completed, to observe states while a search is running. */
    var fetchGate: CompletableDeferred<Unit>? = null

    val currentSession: SearchSession?
        get() = session.value

    val currentResults: List<Video>
        get() = results.value

    val currentRecentQueries: List<String>
        get() = recentQueries.value

    override fun observeResults(): Flow<SearchResults?> =
        combine(session, results) { session, videos ->
            session?.let { SearchResults(it.query, it.order, videos, canLoadMore = it.nextPageToken != null) }
        }

    override fun observeVideo(id: VideoId): Flow<Video?> = results.map { list -> list.find { it.id == id } }

    override suspend fun getSession(): SearchSession? = session.value

    override suspend fun fetchPage(
        query: String,
        order: SearchOrder,
        pageToken: String?,
    ): AppResult<SearchPage, DataError> {
        fetchedPages += Triple(query, order, pageToken)
        fetchGate?.await()
        fetchError?.let { return AppResult.Failure(it) }
        return AppResult.Success(remotePages[query to pageToken] ?: SearchPage(emptyList(), nextPageToken = null))
    }

    override suspend fun saveFirstPage(
        session: SearchSession,
        videos: List<Video>,
    ) {
        results.value = videos.distinctBy { it.id }
        this.session.value = session
    }

    override suspend fun saveNextPage(
        nextPageToken: String?,
        videos: List<Video>,
    ) {
        results.update { stored ->
            val storedIds = stored.map { it.id }.toSet()
            stored + videos.filterNot { it.id in storedIds }
        }
        session.update { it?.copy(nextPageToken = nextPageToken) }
    }

    override fun observeRecentQueries(): Flow<List<String>> = recentQueries

    override suspend fun saveRecentQuery(query: String) {
        recentQueries.update { listOf(query) + (it - query) }
    }

    override suspend fun deleteRecentQuery(query: String) {
        recentQueries.update { it - query }
    }
}
