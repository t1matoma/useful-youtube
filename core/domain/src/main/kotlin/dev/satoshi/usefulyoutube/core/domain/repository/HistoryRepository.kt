package dev.satoshi.usefulyoutube.core.domain.repository

import dev.satoshi.usefulyoutube.core.model.Video
import dev.satoshi.usefulyoutube.core.model.VideoId
import dev.satoshi.usefulyoutube.core.model.WatchedVideo
import kotlinx.coroutines.flow.Flow

/** The watch history is stored only on the device; the Data API does not provide the account's history. */
interface HistoryRepository {
    /** Most recently watched first. */
    fun observeHistory(): Flow<List<WatchedVideo>>

    fun observeVideo(id: VideoId): Flow<Video?>

    /** Puts the video on top of the history with the current time. */
    suspend fun recordWatched(video: Video)

    suspend fun remove(id: VideoId)

    suspend fun clear()
}
