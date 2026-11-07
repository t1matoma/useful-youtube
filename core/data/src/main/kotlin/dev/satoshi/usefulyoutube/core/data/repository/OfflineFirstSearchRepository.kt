package dev.satoshi.usefulyoutube.core.data.repository

import dev.satoshi.usefulyoutube.core.data.di.IoDispatcher
import dev.satoshi.usefulyoutube.core.data.mapper.toEntity
import dev.satoshi.usefulyoutube.core.data.mapper.toFoundVideo
import dev.satoshi.usefulyoutube.core.data.mapper.toSearchResultEntity
import dev.satoshi.usefulyoutube.core.data.mapper.toSearchSession
import dev.satoshi.usefulyoutube.core.data.mapper.toVideo
import dev.satoshi.usefulyoutube.core.database.dao.RecentSearchQueryDao
import dev.satoshi.usefulyoutube.core.database.dao.SearchDao
import dev.satoshi.usefulyoutube.core.domain.repository.SearchPage
import dev.satoshi.usefulyoutube.core.domain.repository.SearchRepository
import dev.satoshi.usefulyoutube.core.domain.repository.SearchSession
import dev.satoshi.usefulyoutube.core.model.AppResult
import dev.satoshi.usefulyoutube.core.model.DataError
import dev.satoshi.usefulyoutube.core.model.SearchOrder
import dev.satoshi.usefulyoutube.core.model.SearchResults
import dev.satoshi.usefulyoutube.core.model.Video
import dev.satoshi.usefulyoutube.core.model.VideoId
import dev.satoshi.usefulyoutube.core.network.VideoSearchRequest
import dev.satoshi.usefulyoutube.core.network.YouTubeNetworkDataSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Provider

/** Search results are stored in Room, so the search screen reads them like any other data. */
internal class OfflineFirstSearchRepository
    @Inject
    constructor(
        private val searchDao: SearchDao,
        private val recentQueryDao: RecentSearchQueryDao,
        private val networkDataSource: YouTubeNetworkDataSource,
        private val locale: Provider<Locale>,
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : SearchRepository {
        override fun observeResults(): Flow<SearchResults?> =
            combine(searchDao.observeSession(), searchDao.observeResults()) { session, results ->
                session?.let {
                    SearchResults(
                        query = it.query,
                        order = it.order,
                        videos = results.map { result -> result.toVideo() },
                        canLoadMore = it.nextPageToken != null,
                    )
                }
            }

        override fun observeVideo(id: VideoId): Flow<Video?> = searchDao.observeResult(id.value).map { it?.toVideo() }

        override suspend fun getSession(): SearchSession? = searchDao.observeSession().first()?.toSearchSession()

        override suspend fun fetchPage(
            query: String,
            order: SearchOrder,
            pageToken: String?,
        ): AppResult<SearchPage, DataError> =
            withContext(ioDispatcher) {
                val deviceLocale = locale.get()
                val request =
                    VideoSearchRequest(
                        query = query,
                        order = order,
                        pageToken = pageToken,
                        regionCode = deviceLocale.country.takeIf { it.length == 2 && it.all(Char::isLetter) },
                        relevanceLanguage = deviceLocale.language.takeIf { it.isNotEmpty() },
                    )
                when (val found = networkDataSource.searchVideos(request)) {
                    is AppResult.Failure -> {
                        found
                    }

                    is AppResult.Success -> {
                        val nextPageToken = found.value.nextPageToken?.takeIf { it.isNotEmpty() }
                        val ids =
                            found.value.items
                                .mapNotNull { it.id.videoId }
                                .distinct()
                        loadDetails(ids).let { details ->
                            when (details) {
                                is AppResult.Failure -> details
                                is AppResult.Success -> AppResult.Success(SearchPage(details.value, nextPageToken))
                            }
                        }
                    }
                }
            }

        /** Keeps the search order; videos the Data API does not return (private, deleted) are skipped. */
        private suspend fun loadDetails(ids: List<String>): AppResult<List<Video>, DataError> {
            if (ids.isEmpty()) return AppResult.Success(emptyList())
            return when (val details = networkDataSource.getVideoDetails(ids)) {
                is AppResult.Failure -> {
                    details
                }

                is AppResult.Success -> {
                    val byId = details.value.associateBy { it.id }
                    AppResult.Success(ids.mapNotNull { byId[it]?.toFoundVideo() })
                }
            }
        }

        override suspend fun saveFirstPage(
            session: SearchSession,
            videos: List<Video>,
        ) = searchDao.replaceSearch(session.toEntity(), videos.map { it.toSearchResultEntity() })

        override suspend fun saveNextPage(
            nextPageToken: String?,
            videos: List<Video>,
        ) = searchDao.appendPage(nextPageToken, videos.map { it.toSearchResultEntity() })

        override fun observeRecentQueries(): Flow<List<String>> = recentQueryDao.observeRecentQueries()

        override suspend fun saveRecentQuery(query: String) =
            recentQueryDao.saveRecentQuery(query, limit = MAXIMUM_RECENT_QUERIES)

        override suspend fun deleteRecentQuery(query: String) = recentQueryDao.deleteRecentQuery(query)

        private companion object {
            const val MAXIMUM_RECENT_QUERIES = 10
        }
    }
