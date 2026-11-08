package dev.satoshi.usefulyoutube.core.testing.repository

import dev.satoshi.usefulyoutube.core.domain.repository.HistoryRepository
import dev.satoshi.usefulyoutube.core.model.PlaybackPosition
import dev.satoshi.usefulyoutube.core.model.Video
import dev.satoshi.usefulyoutube.core.model.VideoId
import dev.satoshi.usefulyoutube.core.model.WatchedVideo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/**
 * Records videos one minute apart, starting at [firstWatchedAt]. Like the database, it keeps playback positions
 * only for videos of the history.
 */
class FakeHistoryRepository(
    initialHistory: List<WatchedVideo> = emptyList(),
    initialPositions: Map<VideoId, PlaybackPosition> = emptyMap(),
    private val firstWatchedAt: Instant = Instant.parse("2026-11-07T10:00:00Z"),
) : HistoryRepository {
    private val history = MutableStateFlow(initialHistory)
    private val positions = MutableStateFlow(initialPositions)
    private var recordCount = 0

    val currentHistory: List<WatchedVideo>
        get() = history.value

    override fun observeHistory(): Flow<List<WatchedVideo>> = history

    override fun observeVideo(id: VideoId): Flow<Video?> =
        history.map { list -> list.find { it.video.id == id }?.video }

    override suspend fun recordWatched(video: Video) {
        val watchedAt = firstWatchedAt + (recordCount++).minutes
        history.update { list -> listOf(WatchedVideo(video, watchedAt)) + list.filterNot { it.video.id == video.id } }
    }

    override fun observePlaybackPositions(): Flow<Map<VideoId, PlaybackPosition>> = positions

    override suspend fun getPlaybackPosition(id: VideoId): PlaybackPosition? = positions.value[id]

    override suspend fun savePlaybackPosition(
        id: VideoId,
        position: PlaybackPosition,
    ) {
        if (history.value.any { it.video.id == id }) positions.update { it + (id to position) }
    }

    override suspend fun remove(id: VideoId) {
        history.update { list -> list.filterNot { it.video.id == id } }
        positions.update { it - id }
    }

    override suspend fun clear() {
        history.value = emptyList()
        positions.value = emptyMap()
    }
}
