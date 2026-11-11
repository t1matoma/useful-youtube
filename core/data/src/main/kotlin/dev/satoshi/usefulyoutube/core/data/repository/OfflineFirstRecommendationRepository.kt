package dev.satoshi.usefulyoutube.core.data.repository

import dev.satoshi.usefulyoutube.core.data.di.IoDispatcher
import dev.satoshi.usefulyoutube.core.data.mapper.toDiscoveredVideo
import dev.satoshi.usefulyoutube.core.data.mapper.toEntity
import dev.satoshi.usefulyoutube.core.data.mapper.toFoundVideo
import dev.satoshi.usefulyoutube.core.data.mapper.toVideo
import dev.satoshi.usefulyoutube.core.database.dao.DiscoveryDao
import dev.satoshi.usefulyoutube.core.database.entity.FeaturedChannelsEntity
import dev.satoshi.usefulyoutube.core.domain.repository.RecommendationRepository
import dev.satoshi.usefulyoutube.core.model.AppResult
import dev.satoshi.usefulyoutube.core.model.ChannelId
import dev.satoshi.usefulyoutube.core.model.DataError
import dev.satoshi.usefulyoutube.core.model.DiscoveredVideo
import dev.satoshi.usefulyoutube.core.model.Video
import dev.satoshi.usefulyoutube.core.model.VideoContentKind
import dev.satoshi.usefulyoutube.core.model.VideoId
import dev.satoshi.usefulyoutube.core.network.ChannelSectionsNetworkDataSource
import dev.satoshi.usefulyoutube.core.network.YouTubeNetworkDataSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

/** Discovered videos are stored in Room like the feed; RSS feeds cost no quota, details 1 unit per 50 videos. */
internal class OfflineFirstRecommendationRepository
    @Inject
    constructor(
        private val discoveryDao: DiscoveryDao,
        private val networkDataSource: YouTubeNetworkDataSource,
        private val channelSections: ChannelSectionsNetworkDataSource,
        private val clock: Clock,
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : RecommendationRepository {
        override fun observeDiscoveredVideos(): Flow<List<DiscoveredVideo>> =
            discoveryDao.observeVideos(WATCHABLE_CONTENT_KINDS).map { videos -> videos.map { it.toDiscoveredVideo() } }

        override fun observeVideo(id: VideoId): Flow<Video?> =
            discoveryDao.observeVideo(id.value).map { it?.video?.toVideo() }

        override suspend fun getFeaturedChannels(channelId: ChannelId): AppResult<List<ChannelId>, DataError> {
            val stored = discoveryDao.getFeaturedChannels(channelId.value)
            if (stored != null && stored.isFresh()) return AppResult.Success(stored.channels())
            return when (
                val result =
                    withContext(
                        ioDispatcher,
                    ) { channelSections.getFeaturedChannelIds(channelId.value) }
            ) {
                is AppResult.Failure -> {
                    // An outdated answer is better than no recommendations.
                    stored?.let { AppResult.Success(it.channels()) } ?: result
                }

                is AppResult.Success -> {
                    discoveryDao.upsertFeaturedChannels(
                        FeaturedChannelsEntity(
                            sourceChannelId = channelId.value,
                            channelIds = result.value.joinToString(CHANNEL_SEPARATOR),
                            fetchedAtEpochMillis = clock.now().toEpochMilliseconds(),
                        ),
                    )
                    AppResult.Success(result.value.map(::ChannelId))
                }
            }
        }

        override suspend fun fetchNewVideos(channelIds: List<ChannelId>): AppResult<List<Video>, DataError> =
            withContext(ioDispatcher) {
                val feeds = loadFeeds(channelIds)
                val failure = feeds.values.firstNotNullOfOrNull { it as? AppResult.Failure }
                if (failure != null && feeds.values.all { it is AppResult.Failure }) return@withContext failure
                val storedIds = discoveryDao.getVideoIds().toSet()
                // A video belongs to the channel whose feed was requested, like in the subscriptions.
                val channelByVideoId =
                    feeds
                        .flatMap { (channelId, feed) ->
                            (feed as? AppResult.Success)?.value.orEmpty().map { videoId -> videoId to channelId }
                        }.filterNot { (videoId, _) -> videoId in storedIds }
                        .toMap()
                if (channelByVideoId.isEmpty()) return@withContext AppResult.Success(emptyList())
                when (val details = networkDataSource.getVideoDetails(channelByVideoId.keys.toList())) {
                    is AppResult.Failure -> {
                        details
                    }

                    is AppResult.Success -> {
                        AppResult.Success(
                            details.value.mapNotNull { item ->
                                item.toFoundVideo()?.copy(channelId = channelByVideoId.getValue(item.id))
                            },
                        )
                    }
                }
            }

        /** Ids of the videos of each channel's RSS feed. */
        private suspend fun loadFeeds(channelIds: List<ChannelId>): Map<ChannelId, AppResult<List<String>, DataError>> =
            coroutineScope {
                val semaphore = Semaphore(MAXIMUM_CONCURRENT_REQUESTS)
                channelIds
                    .map { channelId ->
                        async {
                            semaphore.withPermit {
                                channelId to
                                    when (val feed = networkDataSource.getChannelFeed(channelId.value)) {
                                        is AppResult.Failure -> feed
                                        is AppResult.Success -> AppResult.Success(feed.value.entries.map { it.videoId })
                                    }
                            }
                        }
                    }.awaitAll()
                    .toMap()
            }

        override suspend fun saveDiscoveredVideos(
            videos: List<DiscoveredVideo>,
            channelIds: Set<ChannelId>,
        ) = discoveryDao.save(videos.map { it.toEntity() }, channelIds.map { it.value }.toSet())

        private fun FeaturedChannelsEntity.isFresh(): Boolean =
            clock.now() - Instant.fromEpochMilliseconds(fetchedAtEpochMillis) < FEATURED_CHANNELS_LIFETIME

        private fun FeaturedChannelsEntity.channels(): List<ChannelId> =
            channelIds.split(CHANNEL_SEPARATOR).filter { it.isNotEmpty() }.map(::ChannelId)

        private companion object {
            const val MAXIMUM_CONCURRENT_REQUESTS = 8
            const val CHANNEL_SEPARATOR = ","
            val FEATURED_CHANNELS_LIFETIME = 7.days
            val WATCHABLE_CONTENT_KINDS = setOf(VideoContentKind.REGULAR, VideoContentKind.LIVE_OR_PREMIERE)
        }
    }
