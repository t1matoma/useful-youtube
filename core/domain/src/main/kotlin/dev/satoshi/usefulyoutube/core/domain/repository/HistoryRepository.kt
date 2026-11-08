package dev.satoshi.usefulyoutube.core.domain.repository

import dev.satoshi.usefulyoutube.core.model.PlaybackPosition
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

    /** Videos of the history whose playback position was saved. */
    fun observePlaybackPositions(): Flow<Map<VideoId, PlaybackPosition>>

    /** `null` when the video is not in the history or its position was not saved. */
    suspend fun getPlaybackPosition(id: VideoId): PlaybackPosition?

    /** Does nothing for a video that is not in the history; removing the video removes its position. */
    suspend fun savePlaybackPosition(
        id: VideoId,
        position: PlaybackPosition,
    )

    suspend fun remove(id: VideoId)

    suspend fun clear()
}
