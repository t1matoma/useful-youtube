package dev.satoshi.usefulyoutube.core.data.repository

import dev.satoshi.usefulyoutube.core.data.mapper.toSnapshot
import dev.satoshi.usefulyoutube.core.data.mapper.toVideo
import dev.satoshi.usefulyoutube.core.database.dao.WatchHistoryDao
import dev.satoshi.usefulyoutube.core.database.entity.WatchHistoryEntity
import dev.satoshi.usefulyoutube.core.domain.repository.HistoryRepository
import dev.satoshi.usefulyoutube.core.model.Video
import dev.satoshi.usefulyoutube.core.model.VideoId
import dev.satoshi.usefulyoutube.core.model.WatchedVideo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.Instant

internal class OfflineFirstHistoryRepository
    @Inject
    constructor(
        private val historyDao: WatchHistoryDao,
        private val clock: Clock,
    ) : HistoryRepository {
        override fun observeHistory(): Flow<List<WatchedVideo>> =
            historyDao.observeHistory().map { entries ->
                entries.map { WatchedVideo(it.video.toVideo(), Instant.fromEpochMilliseconds(it.watchedAtEpochMillis)) }
            }

        override fun observeVideo(id: VideoId): Flow<Video?> =
            historyDao.observeEntry(id.value).map { it?.video?.toVideo() }

        override suspend fun recordWatched(video: Video) =
            historyDao.record(
                WatchHistoryEntity(clock.now().toEpochMilliseconds(), video.toSnapshot()),
                limit = MAXIMUM_HISTORY_SIZE,
            )

        override suspend fun remove(id: VideoId) = historyDao.delete(id.value)

        override suspend fun clear() = historyDao.clear()

        private companion object {
            const val MAXIMUM_HISTORY_SIZE = 500
        }
    }
