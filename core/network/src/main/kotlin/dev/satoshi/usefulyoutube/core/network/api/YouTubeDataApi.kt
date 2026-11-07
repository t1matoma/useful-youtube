package dev.satoshi.usefulyoutube.core.network.api

import dev.satoshi.usefulyoutube.core.network.model.ChannelListResponseDto
import dev.satoshi.usefulyoutube.core.network.model.SearchListResponseDto
import dev.satoshi.usefulyoutube.core.network.model.SubscriptionListResponseDto
import dev.satoshi.usefulyoutube.core.network.model.VideoListResponseDto
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

/**
 * YouTube Data API v3. The API key is added by [ApiKeyInterceptor]; requests about the user's account
 * also carry an OAuth access token.
 */
interface YouTubeDataApi {
    @GET("videos")
    suspend fun getVideos(
        @Query("id") commaSeparatedIds: String,
        @Query("part") part: String = "contentDetails,snippet",
        @Query("maxResults") maxResults: Int = MAXIMUM_IDS_PER_REQUEST,
    ): VideoListResponseDto

    @GET("channels")
    suspend fun getChannelByHandle(
        @Query("forHandle") handle: String,
        @Query("part") part: String = "snippet",
    ): ChannelListResponseDto

    @GET("subscriptions")
    suspend fun getMySubscriptions(
        @Header("Authorization") authorization: String,
        @Query("pageToken") pageToken: String?,
        @Query("mine") mine: Boolean = true,
        @Query("part") part: String = "snippet",
        @Query("maxResults") maxResults: Int = MAXIMUM_RESULTS_PER_PAGE,
    ): SubscriptionListResponseDto

    /** Costs 100 quota units per page, regardless of `maxResults`. */
    @GET("search")
    suspend fun searchVideos(
        @Query("q") query: String,
        @Query("order") order: String,
        @Query("pageToken") pageToken: String?,
        @Query("regionCode") regionCode: String?,
        @Query("relevanceLanguage") relevanceLanguage: String?,
        @Query("type") type: String = "video",
        @Query("part") part: String = "id",
        @Query("maxResults") maxResults: Int = MAXIMUM_RESULTS_PER_PAGE,
    ): SearchListResponseDto

    companion object {
        const val MAXIMUM_IDS_PER_REQUEST = 50
        const val MAXIMUM_RESULTS_PER_PAGE = 50
    }
}
