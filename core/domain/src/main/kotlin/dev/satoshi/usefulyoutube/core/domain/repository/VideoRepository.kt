package dev.satoshi.usefulyoutube.core.domain.repository

import dev.satoshi.usefulyoutube.core.domain.shorts.ShortsCandidate
import dev.satoshi.usefulyoutube.core.model.AppResult
import dev.satoshi.usefulyoutube.core.model.ChannelId
import dev.satoshi.usefulyoutube.core.model.DataError
import dev.satoshi.usefulyoutube.core.model.Video
import dev.satoshi.usefulyoutube.core.model.VideoContentKind
import dev.satoshi.usefulyoutube.core.model.VideoId
import kotlinx.coroutines.flow.Flow

interface VideoRepository {
    /** Regular videos, live streams and premieres of all subscriptions, newest first. */
    fun observeFeed(): Flow<List<Video>>

    fun observeVideo(id: VideoId): Flow<Video?>

    /** Loads the latest videos of the channel from its RSS feed; returns the number of new videos. */
    suspend fun syncChannelFeed(channelId: ChannelId): AppResult<Int, DataError>

    /** Loads durations and descriptions for videos that do not have them yet. */
    suspend fun fetchMissingDetails(): AppResult<Unit, DataError>

    /** Videos with loaded details whose kind is not determined yet. */
    suspend fun getUnclassifiedVideos(): List<ShortsCandidate>

    suspend fun saveContentKinds(kinds: Map<VideoId, VideoContentKind>)
}
