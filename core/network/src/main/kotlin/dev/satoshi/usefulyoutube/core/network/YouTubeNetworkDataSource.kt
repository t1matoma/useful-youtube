package dev.satoshi.usefulyoutube.core.network

import dev.satoshi.usefulyoutube.core.model.AppResult
import dev.satoshi.usefulyoutube.core.model.DataError
import dev.satoshi.usefulyoutube.core.model.SearchOrder
import dev.satoshi.usefulyoutube.core.network.model.ChannelItemDto
import dev.satoshi.usefulyoutube.core.network.model.RssFeedDto
import dev.satoshi.usefulyoutube.core.network.model.SearchListResponseDto
import dev.satoshi.usefulyoutube.core.network.model.SubscriptionItemDto
import dev.satoshi.usefulyoutube.core.network.model.VideoItemDto

/**
 * Access to the official YouTube sources: channel RSS feeds, the Data API (with an API key or the user's
 * OAuth access token) and Shorts URLs.
 */
interface YouTubeNetworkDataSource {
    suspend fun getChannelFeed(channelId: String): AppResult<RssFeedDto, DataError>

    /** Loads details of any number of videos, requesting them in batches of the Data API limit. */
    suspend fun getVideoDetails(videoIds: List<String>): AppResult<List<VideoItemDto>, DataError>

    suspend fun getChannelByHandle(handle: String): AppResult<ChannelItemDto?, DataError>

    /** Loads all subscriptions of the account that granted the [accessToken], following every page. */
    suspend fun getMySubscriptions(accessToken: String): AppResult<List<SubscriptionItemDto>, DataError>

    /** One page of videos found on YouTube; [pageToken] is `null` for the first page. */
    suspend fun searchVideos(request: VideoSearchRequest): AppResult<SearchListResponseDto, DataError>

    /** `true` when `/shorts/{id}` opens without a redirect to the regular watch page. */
    suspend fun isShortsUrl(videoId: String): AppResult<Boolean, DataError>
}

/**
 * Parameters of a video search. [regionCode] (ISO 3166-1 alpha-2) and [relevanceLanguage] (ISO 639-1)
 * make the results match what YouTube shows for the device locale.
 */
data class VideoSearchRequest(
    val query: String,
    val order: SearchOrder,
    val pageToken: String? = null,
    val regionCode: String? = null,
    val relevanceLanguage: String? = null,
)
