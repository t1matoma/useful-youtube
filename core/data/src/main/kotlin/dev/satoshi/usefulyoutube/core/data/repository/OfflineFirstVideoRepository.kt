package dev.satoshi.usefulyoutube.core.data.repository

import dev.satoshi.usefulyoutube.core.data.di.IoDispatcher
import dev.satoshi.usefulyoutube.core.data.mapper.toDetailsUpdate
import dev.satoshi.usefulyoutube.core.data.mapper.toShortsCandidate
import dev.satoshi.usefulyoutube.core.data.mapper.toVideo
import dev.satoshi.usefulyoutube.core.data.mapper.toVideoEntity
import dev.satoshi.usefulyoutube.core.database.dao.VideoDao
import dev.satoshi.usefulyoutube.core.database.entity.VideoContentKindUpdate
import dev.satoshi.usefulyoutube.core.domain.repository.VideoRepository
import dev.satoshi.usefulyoutube.core.domain.shorts.ShortsCandidate
import dev.satoshi.usefulyoutube.core.model.AppResult
import dev.satoshi.usefulyoutube.core.model.ChannelId
import dev.satoshi.usefulyoutube.core.model.DataError
import dev.satoshi.usefulyoutube.core.model.Video
import dev.satoshi.usefulyoutube.core.model.VideoContentKind
import dev.satoshi.usefulyoutube.core.model.VideoId
import dev.satoshi.usefulyoutube.core.network.YouTubeNetworkDataSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Room is the single source of truth; the network only updates it. */
internal class OfflineFirstVideoRepository
    @Inject
    constructor(
        private val videoDao: VideoDao,
        private val networkDataSource: YouTubeNetworkDataSource,
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : VideoRepository {
        override fun observeFeed(): Flow<List<Video>> =
            videoDao.observeVideos(FEED_CONTENT_KINDS).map { videos -> videos.map { it.toVideo() } }

        override fun observeVideo(id: VideoId): Flow<Video?> = videoDao.observeVideo(id.value).map { it?.toVideo() }

        override suspend fun syncChannelFeed(channelId: ChannelId): AppResult<Int, DataError> =
            when (val feed = withContext(ioDispatcher) { networkDataSource.getChannelFeed(channelId.value) }) {
                is AppResult.Failure -> {
                    feed
                }

                is AppResult.Success -> {
                    val entities = feed.value.entries.mapNotNull { it.toVideoEntity() }
                    AppResult.Success(videoDao.insertOrIgnore(entities).count { it != ALREADY_EXISTS })
                }
            }

        override suspend fun fetchMissingDetails(): AppResult<Unit, DataError> {
            val ids = videoDao.getIdsWithoutDetails()
            if (ids.isEmpty()) return AppResult.Success(Unit)
            return when (val details = withContext(ioDispatcher) { networkDataSource.getVideoDetails(ids) }) {
                is AppResult.Failure -> {
                    details
                }

                is AppResult.Success -> {
                    videoDao.updateDetails(details.value.map { it.toDetailsUpdate() })
                    AppResult.Success(Unit)
                }
            }
        }

        override suspend fun getUnclassifiedVideos(): List<ShortsCandidate> =
            videoDao.getUnclassifiedVideos().map { it.toShortsCandidate() }

        override suspend fun saveContentKinds(kinds: Map<VideoId, VideoContentKind>) =
            videoDao.updateContentKinds(kinds.map { (id, kind) -> VideoContentKindUpdate(id.value, kind) })

        private companion object {
            const val ALREADY_EXISTS = -1L
            val FEED_CONTENT_KINDS = setOf(VideoContentKind.REGULAR, VideoContentKind.LIVE_OR_PREMIERE)
        }
    }
