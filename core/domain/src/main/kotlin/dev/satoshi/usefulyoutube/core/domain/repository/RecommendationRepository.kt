package dev.satoshi.usefulyoutube.core.domain.repository

import dev.satoshi.usefulyoutube.core.model.AppResult
import dev.satoshi.usefulyoutube.core.model.ChannelId
import dev.satoshi.usefulyoutube.core.model.DataError
import dev.satoshi.usefulyoutube.core.model.DiscoveredVideo
import dev.satoshi.usefulyoutube.core.model.Video
import dev.satoshi.usefulyoutube.core.model.VideoId
import kotlinx.coroutines.flow.Flow

/** Videos of channels outside the subscriptions that the recommendations can offer. */
interface RecommendationRepository {
    /** Stored regular videos, live streams and premieres; Shorts are kept only to not load them again. */
    fun observeDiscoveredVideos(): Flow<List<DiscoveredVideo>>

    fun observeVideo(id: VideoId): Flow<Video?>

    /**
     * Channels that [channelId] features on its page (`channelSections.list`, 1 quota unit). The answer is
     * stored and reused for a week.
     */
    suspend fun getFeaturedChannels(channelId: ChannelId): AppResult<List<ChannelId>, DataError>

    /**
     * Loads the RSS feeds of the channels and the details of their videos that are not stored yet; the kinds
     * are not determined. A channel whose feed fails is skipped.
     */
    suspend fun fetchNewVideos(channelIds: List<ChannelId>): AppResult<List<Video>, DataError>

    /** Adds the [videos] and forgets the stored videos of channels that are not among [channelIds]. */
    suspend fun saveDiscoveredVideos(
        videos: List<DiscoveredVideo>,
        channelIds: Set<ChannelId>,
    )
}
