package dev.satoshi.usefulyoutube.core.network

import dev.satoshi.usefulyoutube.core.model.AppResult
import dev.satoshi.usefulyoutube.core.model.DataError
import dev.satoshi.usefulyoutube.core.network.model.ChannelItemDto
import dev.satoshi.usefulyoutube.core.network.model.RssFeedDto
import dev.satoshi.usefulyoutube.core.network.model.VideoItemDto

/** Access to the official public YouTube sources: channel RSS feeds, the Data API and Shorts URLs. */
interface YouTubeNetworkDataSource {
    suspend fun getChannelFeed(channelId: String): AppResult<RssFeedDto, DataError>

    /** Loads details of any number of videos, requesting them in batches of the Data API limit. */
    suspend fun getVideoDetails(videoIds: List<String>): AppResult<List<VideoItemDto>, DataError>

    suspend fun getChannelByHandle(handle: String): AppResult<ChannelItemDto?, DataError>

    /** `true` when `/shorts/{id}` opens without a redirect to the regular watch page. */
    suspend fun isShortsUrl(videoId: String): AppResult<Boolean, DataError>
}
