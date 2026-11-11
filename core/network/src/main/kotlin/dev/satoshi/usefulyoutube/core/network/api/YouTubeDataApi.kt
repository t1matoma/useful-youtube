package dev.satoshi.usefulyoutube.core.network.api

import dev.satoshi.usefulyoutube.core.network.model.ChannelListResponseDto
import dev.satoshi.usefulyoutube.core.network.model.ChannelSectionListResponseDto
import dev.satoshi.usefulyoutube.core.network.model.PlaylistItemListResponseDto
import dev.satoshi.usefulyoutube.core.network.model.PlaylistListResponseDto
import dev.satoshi.usefulyoutube.core.network.model.SearchListResponseDto
import dev.satoshi.usefulyoutube.core.network.model.SubscriptionListResponseDto
import dev.satoshi.usefulyoutube.core.network.model.VideoListResponseDto
import dev.satoshi.usefulyoutube.core.network.model.VideoRatingListResponseDto
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
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

    @GET("playlists")
    suspend fun getMyPlaylists(
        @Header("Authorization") authorization: String,
        @Query("pageToken") pageToken: String?,
        @Query("mine") mine: Boolean = true,
        @Query("part") part: String = "snippet",
        @Query("maxResults") maxResults: Int = MAXIMUM_RESULTS_PER_PAGE,
    ): PlaylistListResponseDto

    @GET("playlistItems")
    suspend fun getPlaylistItems(
        @Header("Authorization") authorization: String,
        @Query("playlistId") playlistId: String,
        @Query("pageToken") pageToken: String?,
        @Query("part") part: String = "contentDetails",
        @Query("maxResults") maxResults: Int = MAXIMUM_RESULTS_PER_PAGE,
    ): PlaylistItemListResponseDto

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

    @GET("videos/getRating")
    suspend fun getVideoRating(
        @Header("Authorization") authorization: String,
        @Query("id") videoId: String,
    ): VideoRatingListResponseDto

    /** Costs 50 quota units; answers with an empty body. */
    @POST("videos/rate")
    suspend fun rateVideo(
        @Header("Authorization") authorization: String,
        @Query("id") videoId: String,
        @Query("rating") rating: String,
    )

    @GET("channelSections")
    suspend fun getChannelSections(
        @Query("channelId") channelId: String,
        @Query("part") part: String = "snippet,contentDetails",
    ): ChannelSectionListResponseDto

    companion object {
        const val MAXIMUM_IDS_PER_REQUEST = 50
        const val MAXIMUM_RESULTS_PER_PAGE = 50
    }
}
