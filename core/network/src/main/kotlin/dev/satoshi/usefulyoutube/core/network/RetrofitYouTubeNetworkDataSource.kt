package dev.satoshi.usefulyoutube.core.network

import dev.satoshi.usefulyoutube.core.model.AppResult
import dev.satoshi.usefulyoutube.core.model.DataError
import dev.satoshi.usefulyoutube.core.model.SearchOrder
import dev.satoshi.usefulyoutube.core.network.api.YouTubeDataApi
import dev.satoshi.usefulyoutube.core.network.di.ShortsProbeClient
import dev.satoshi.usefulyoutube.core.network.model.ChannelItemDto
import dev.satoshi.usefulyoutube.core.network.model.PlaylistDto
import dev.satoshi.usefulyoutube.core.network.model.RssFeedDto
import dev.satoshi.usefulyoutube.core.network.model.SearchListResponseDto
import dev.satoshi.usefulyoutube.core.network.model.SubscriptionItemDto
import dev.satoshi.usefulyoutube.core.network.model.VideoItemDto
import dev.satoshi.usefulyoutube.core.network.rss.RssFeedParser
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.coroutines.executeAsync
import javax.inject.Inject

internal class RetrofitYouTubeNetworkDataSource
    @Inject
    constructor(
        private val api: YouTubeDataApi,
        private val httpClient: OkHttpClient,
        @param:ShortsProbeClient private val shortsProbeClient: OkHttpClient,
        private val rssFeedParser: RssFeedParser,
        private val endpoints: YouTubeEndpoints,
        json: Json,
    ) : YouTubeNetworkDataSource {
        private val errorMapper = NetworkErrorMapper(json)

        override suspend fun getChannelFeed(channelId: String): AppResult<RssFeedDto, DataError> =
            errorMapper.catching {
                val url =
                    endpoints.websiteBaseUrl
                        .newBuilder()
                        .addPathSegments("feeds/videos.xml")
                        .addQueryParameter("channel_id", channelId)
                        .build()
                httpClient.newCall(Request.Builder().url(url).build()).executeAsync().use { response ->
                    if (!response.isSuccessful) return@catching AppResult.Failure(errorMapper.fromStatus(response.code))
                    AppResult.Success(rssFeedParser.parse(response.body.string()))
                }
            }

        override suspend fun getVideoDetails(videoIds: List<String>): AppResult<List<VideoItemDto>, DataError> =
            errorMapper.catching {
                val items =
                    videoIds.chunked(YouTubeDataApi.MAXIMUM_IDS_PER_REQUEST).flatMap { batch ->
                        api.getVideos(commaSeparatedIds = batch.joinToString(separator = ",")).items
                    }
                AppResult.Success(items)
            }

        override suspend fun getChannelByHandle(handle: String): AppResult<ChannelItemDto?, DataError> =
            errorMapper.catching {
                AppResult.Success(api.getChannelByHandle(handle = "@${handle.removePrefix("@")}").items.firstOrNull())
            }

        override suspend fun getMySubscriptions(accessToken: String): AppResult<List<SubscriptionItemDto>, DataError> =
            errorMapper.catching {
                AppResult.Success(
                    allPages { pageToken ->
                        api.getMySubscriptions(bearer(accessToken), pageToken).let { it.items to it.nextPageToken }
                    },
                )
            }

        override suspend fun getMyPlaylists(accessToken: String): AppResult<List<PlaylistDto>, DataError> =
            errorMapper.catching {
                AppResult.Success(
                    allPages { pageToken ->
                        api.getMyPlaylists(bearer(accessToken), pageToken).let { it.items to it.nextPageToken }
                    },
                )
            }

        override suspend fun getPlaylistVideoIds(
            accessToken: String,
            playlistId: String,
        ): AppResult<List<String>, DataError> =
            errorMapper.catching {
                AppResult.Success(
                    allPages { pageToken ->
                        api
                            .getPlaylistItems(bearer(accessToken), playlistId, pageToken)
                            .let { page -> page.items.map { it.contentDetails.videoId } to page.nextPageToken }
                    },
                )
            }

        /** Loads pages until the Data API returns no `nextPageToken`. */
        private suspend fun <T> allPages(loadPage: suspend (pageToken: String?) -> Pair<List<T>, String?>): List<T> {
            val items = mutableListOf<T>()
            var pageToken: String? = null
            do {
                val (pageItems, nextPageToken) = loadPage(pageToken)
                items += pageItems
                pageToken = nextPageToken?.takeIf { it.isNotEmpty() }
            } while (pageToken != null)
            return items
        }

        private fun bearer(accessToken: String) = "Bearer $accessToken"

        override suspend fun searchVideos(request: VideoSearchRequest): AppResult<SearchListResponseDto, DataError> =
            errorMapper.catching {
                AppResult.Success(
                    api.searchVideos(
                        query = request.query,
                        order = request.order.apiValue,
                        pageToken = request.pageToken,
                        regionCode = request.regionCode,
                        relevanceLanguage = request.relevanceLanguage,
                    ),
                )
            }

        override suspend fun isShortsUrl(videoId: String): AppResult<Boolean, DataError> =
            errorMapper.catching {
                val url = endpoints.websiteBaseUrl.resolve("shorts/$videoId") ?: error("Invalid video id: $videoId")
                val request =
                    Request
                        .Builder()
                        .url(url)
                        .head()
                        .build()
                shortsProbeClient.newCall(request).executeAsync().use { response ->
                    val redirectsToWatchPage =
                        response.isRedirect && response.header(LOCATION_HEADER).orEmpty().contains(WATCH_PATH)
                    when {
                        response.isSuccessful -> AppResult.Success(true)

                        redirectsToWatchPage -> AppResult.Success(false)

                        // Other redirects (for example, a cookie consent page) do not tell anything about the video.
                        else -> AppResult.Failure(errorMapper.fromStatus(response.code))
                    }
                }
            }

        private val SearchOrder.apiValue: String
            get() =
                when (this) {
                    SearchOrder.RELEVANCE -> "relevance"
                    SearchOrder.DATE -> "date"
                }

        private companion object {
            const val LOCATION_HEADER = "Location"
            const val WATCH_PATH = "/watch"
        }
    }
