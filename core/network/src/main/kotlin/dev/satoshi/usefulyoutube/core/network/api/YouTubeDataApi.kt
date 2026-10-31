package dev.satoshi.usefulyoutube.core.network.api

import dev.satoshi.usefulyoutube.core.network.model.ChannelListResponseDto
import dev.satoshi.usefulyoutube.core.network.model.VideoListResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

/** YouTube Data API v3. The API key is added by [ApiKeyInterceptor]. */
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

    companion object {
        const val MAXIMUM_IDS_PER_REQUEST = 50
    }
}
