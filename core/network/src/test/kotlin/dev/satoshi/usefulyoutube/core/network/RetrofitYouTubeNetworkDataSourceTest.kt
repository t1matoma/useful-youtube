package dev.satoshi.usefulyoutube.core.network

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNull
import assertk.assertions.prop
import dev.satoshi.usefulyoutube.core.model.AppResult
import dev.satoshi.usefulyoutube.core.model.DataError
import dev.satoshi.usefulyoutube.core.model.SearchOrder
import dev.satoshi.usefulyoutube.core.network.api.ApiKeyInterceptor
import dev.satoshi.usefulyoutube.core.network.api.YouTubeDataApi
import dev.satoshi.usefulyoutube.core.network.model.RssFeedDto
import dev.satoshi.usefulyoutube.core.network.model.VideoItemDto
import dev.satoshi.usefulyoutube.core.network.rss.RssFeedParser
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class RetrofitYouTubeNetworkDataSourceTest {
    private val server = MockWebServer()
    private lateinit var dataSource: RetrofitYouTubeNetworkDataSource

    @Before
    fun setUp() {
        server.start()
        val json = Json { ignoreUnknownKeys = true }
        val client = OkHttpClient()
        val endpoints = YouTubeEndpoints(dataApiBaseUrl = server.url("/youtube/v3/"), websiteBaseUrl = server.url("/"))
        val api =
            Retrofit
                .Builder()
                .baseUrl(endpoints.dataApiBaseUrl)
                .client(client.newBuilder().addInterceptor(ApiKeyInterceptor(API_KEY)).build())
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(YouTubeDataApi::class.java)
        dataSource =
            RetrofitYouTubeNetworkDataSource(
                api = api,
                httpClient = client,
                shortsProbeClient = client.newBuilder().followRedirects(false).build(),
                rssFeedParser = RssFeedParser(),
                endpoints = endpoints,
                json = json,
            )
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun enqueue(
        code: Int,
        body: String = "",
        vararg headers: Pair<String, String>,
    ) {
        val builder = MockResponse.Builder().code(code).body(body)
        headers.forEach { (name, value) -> builder.addHeader(name, value) }
        server.enqueue(builder.build())
    }

    @Test
    fun `channel feed is requested by channel id and parsed`() =
        runTest {
            enqueue(code = 200, body = Fixtures.read("channel_feed.xml"))

            val result = dataSource.getChannelFeed("UC_x5XG1OV2P6uZZ5FSM9Ttw")

            val request = server.takeRequest()
            assertThat(request.url.encodedPath).isEqualTo("/feeds/videos.xml")
            assertThat(request.url.queryParameter("channel_id")).isEqualTo("UC_x5XG1OV2P6uZZ5FSM9Ttw")
            assertThat(result)
                .isInstanceOf<AppResult.Success<RssFeedDto>>()
                .prop(AppResult.Success<RssFeedDto>::value)
                .prop(RssFeedDto::title)
                .isEqualTo("Google for Developers")
        }

    @Test
    fun `missing channel feed is not found`() =
        runTest {
            enqueue(code = 404)

            assertThat(dataSource.getChannelFeed("UCmissing")).isEqualTo(AppResult.Failure(DataError.NotFound))
        }

    @Test
    fun `invalid feed content is a parsing error`() =
        runTest {
            enqueue(code = 200, body = "<html></html>")

            assertThat(dataSource.getChannelFeed("UCbroken")).isEqualTo(AppResult.Failure(DataError.Parsing))
        }

    @Test
    fun `connection failure is no network`() =
        runTest {
            server.close()

            assertThat(dataSource.getChannelFeed("UCoffline")).isEqualTo(AppResult.Failure(DataError.NoNetwork))
        }

    @Test
    fun `video details are requested with api key and parts`() =
        runTest {
            enqueue(code = 200, body = Fixtures.read("videos_response.json"))

            val result = dataSource.getVideoDetails(listOf("aaaaaaaaaaa", "ccccccccccc"))

            val request = server.takeRequest()
            assertThat(request.url.encodedPath).isEqualTo("/youtube/v3/videos")
            assertThat(request.url.queryParameter("key")).isEqualTo(API_KEY)
            assertThat(request.url.queryParameter("part")).isEqualTo("contentDetails,snippet")
            assertThat(request.url.queryParameter("id")).isEqualTo("aaaaaaaaaaa,ccccccccccc")
            val items = (result as AppResult.Success).value
            assertThat(items.map { it.id }).containsExactly("aaaaaaaaaaa", "ccccccccccc")
            assertThat(items.map { it.contentDetails?.duration }).containsExactly("PT12M31S", "P0D")
            assertThat(items.first().snippet.description).isEqualTo("Full description")
        }

    @Test
    fun `video details are requested in batches of fifty`() =
        runTest {
            enqueue(code = 200, body = """{"items": []}""")
            enqueue(code = 200, body = """{"items": []}""")
            val ids = List(size = 51) { index -> "video$index" }

            val result = dataSource.getVideoDetails(ids)

            assertThat(result).isEqualTo(AppResult.Success(emptyList<VideoItemDto>()))
            assertThat(
                server
                    .takeRequest()
                    .url
                    .queryParameter("id")
                    ?.split(",")
                    ?.size,
            ).isEqualTo(50)
            assertThat(server.takeRequest().url.queryParameter("id")).isEqualTo("video50")
        }

    @Test
    fun `no request is made for empty id list`() =
        runTest {
            assertThat(dataSource.getVideoDetails(emptyList())).isEqualTo(AppResult.Success(emptyList<VideoItemDto>()))
            assertThat(server.requestCount).isEqualTo(0)
        }

    @Test
    fun `exceeded quota is reported`() =
        runTest {
            enqueue(code = 403, body = Fixtures.read("error_quota_exceeded.json"))

            assertThat(dataSource.getVideoDetails(listOf("a"))).isEqualTo(AppResult.Failure(DataError.QuotaExceeded))
        }

    @Test
    fun `invalid api key is reported`() =
        runTest {
            enqueue(code = 400, body = Fixtures.read("error_key_invalid.json"))

            assertThat(dataSource.getVideoDetails(listOf("a"))).isEqualTo(AppResult.Failure(DataError.InvalidApiKey))
        }

    @Test
    fun `unknown http error keeps status code`() =
        runTest {
            enqueue(code = 500, body = "Internal error")

            assertThat(dataSource.getVideoDetails(listOf("a"))).isEqualTo(AppResult.Failure(DataError.Http(500)))
        }

    @Test
    fun `channel is found by handle`() =
        runTest {
            enqueue(code = 200, body = Fixtures.read("channels_response.json"))

            val result = dataSource.getChannelByHandle("googledevelopers")

            val request = server.takeRequest()
            assertThat(request.url.encodedPath).isEqualTo("/youtube/v3/channels")
            assertThat(request.url.queryParameter("forHandle")).isEqualTo("@googledevelopers")
            assertThat((result as AppResult.Success).value?.snippet?.title).isEqualTo("Google for Developers")
        }

    @Test
    fun `unknown handle gives no channel`() =
        runTest {
            enqueue(code = 200, body = Fixtures.read("empty_channels_response.json"))

            assertThat((dataSource.getChannelByHandle("@nobody") as AppResult.Success).value).isNull()
        }

    @Test
    fun `account subscriptions follow every page with bearer token`() =
        runTest {
            enqueue(code = 200, body = Fixtures.read("subscriptions_page_1.json"))
            enqueue(code = 200, body = Fixtures.read("subscriptions_page_2.json"))

            val result = dataSource.getMySubscriptions(accessToken = "access-token")

            val first = server.takeRequest()
            assertThat(first.url.encodedPath).isEqualTo("/youtube/v3/subscriptions")
            assertThat(first.headers["Authorization"]).isEqualTo("Bearer access-token")
            assertThat(first.url.queryParameter("mine")).isEqualTo("true")
            assertThat(first.url.queryParameter("part")).isEqualTo("snippet")
            assertThat(first.url.queryParameter("maxResults")).isEqualTo("50")
            assertThat(first.url.queryParameter("pageToken")).isNull()
            assertThat(server.takeRequest().url.queryParameter("pageToken")).isEqualTo("CAEQAA")
            val snippets = (result as AppResult.Success).value.map { it.snippet }
            assertThat(snippets.map { it.resourceId.channelId })
                .containsExactly("UC_x5XG1OV2P6uZZ5FSM9Ttw", "UCVHFbqXqoYvEWM1Ddxl0QDg")
            assertThat(snippets.map { it.title }).containsExactly("Google for Developers", "Android Developers")
        }

    @Test
    fun `account playlists are requested with bearer token`() =
        runTest {
            enqueue(code = 200, body = Fixtures.read("playlists_response.json"))

            val result = dataSource.getMyPlaylists(accessToken = "access-token")

            val request = server.takeRequest()
            assertThat(request.url.encodedPath).isEqualTo("/youtube/v3/playlists")
            assertThat(request.headers["Authorization"]).isEqualTo("Bearer access-token")
            assertThat(request.url.queryParameter("mine")).isEqualTo("true")
            assertThat(request.url.queryParameter("part")).isEqualTo("snippet")
            val playlists = (result as AppResult.Success).value
            assertThat(playlists.map { it.id })
                .containsExactly("PLaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", "PLbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb")
            assertThat(playlists.map { it.snippet.title }).containsExactly("Kotlin talks", "Music")
        }

    @Test
    fun `playlist video ids follow every page`() =
        runTest {
            enqueue(code = 200, body = Fixtures.read("playlist_items_page_1.json"))
            enqueue(code = 200, body = Fixtures.read("playlist_items_page_2.json"))

            val result = dataSource.getPlaylistVideoIds(accessToken = "access-token", playlistId = "PLaaaa")

            val first = server.takeRequest()
            assertThat(first.url.encodedPath).isEqualTo("/youtube/v3/playlistItems")
            assertThat(first.url.queryParameter("playlistId")).isEqualTo("PLaaaa")
            assertThat(first.url.queryParameter("part")).isEqualTo("contentDetails")
            assertThat(first.headers["Authorization"]).isEqualTo("Bearer access-token")
            assertThat(server.takeRequest().url.queryParameter("pageToken")).isEqualTo("EAAaBlBUOkNBVQ")
            assertThat((result as AppResult.Success).value)
                .containsExactly("aaaaaaaaaaa", "bbbbbbbbbbb", "ccccccccccc")
        }

    @Test
    fun `rejected access token is unauthorized`() =
        runTest {
            enqueue(code = 401, body = Fixtures.read("error_auth.json"))

            assertThat(dataSource.getMySubscriptions(accessToken = "expired"))
                .isEqualTo(AppResult.Failure(DataError.Unauthorized))
        }

    @Test
    fun `video search sends query, order and locale`() =
        runTest {
            enqueue(code = 200, body = Fixtures.read("search_response.json"))

            val result =
                dataSource.searchVideos(
                    VideoSearchRequest(
                        query = "kotlin coroutines",
                        order = SearchOrder.DATE,
                        regionCode = "DE",
                        relevanceLanguage = "de",
                    ),
                )

            val request = server.takeRequest()
            assertThat(request.url.encodedPath).isEqualTo("/youtube/v3/search")
            assertThat(request.url.queryParameter("q")).isEqualTo("kotlin coroutines")
            assertThat(request.url.queryParameter("order")).isEqualTo("date")
            assertThat(request.url.queryParameter("type")).isEqualTo("video")
            assertThat(request.url.queryParameter("part")).isEqualTo("id")
            assertThat(request.url.queryParameter("maxResults")).isEqualTo("50")
            assertThat(request.url.queryParameter("regionCode")).isEqualTo("DE")
            assertThat(request.url.queryParameter("relevanceLanguage")).isEqualTo("de")
            assertThat(request.url.queryParameter("pageToken")).isNull()
            assertThat(request.url.queryParameter("key")).isEqualTo(API_KEY)
            val page = (result as AppResult.Success).value
            assertThat(page.items.map { it.id.videoId }).containsExactly("aaaaaaaaaaa", "ccccccccccc")
            assertThat(page.nextPageToken).isEqualTo("CDIQAA")
        }

    @Test
    fun `next search page is requested by token without empty locale parameters`() =
        runTest {
            enqueue(code = 200, body = Fixtures.read("empty_search_response.json"))

            val result =
                dataSource.searchVideos(
                    VideoSearchRequest(query = "q", order = SearchOrder.RELEVANCE, pageToken = "CDIQAA"),
                )

            val request = server.takeRequest()
            assertThat(request.url.queryParameter("order")).isEqualTo("relevance")
            assertThat(request.url.queryParameter("pageToken")).isEqualTo("CDIQAA")
            assertThat(request.url.queryParameterNames.contains("regionCode")).isEqualTo(false)
            assertThat(request.url.queryParameterNames.contains("relevanceLanguage")).isEqualTo(false)
            assertThat((result as AppResult.Success).value.nextPageToken).isNull()
        }

    @Test
    fun `shorts url without redirect is a short`() =
        runTest {
            enqueue(code = 200)

            val result = dataSource.isShortsUrl("bbbbbbbbbbb")

            val request = server.takeRequest()
            assertThat(request.method).isEqualTo("HEAD")
            assertThat(request.url.encodedPath).isEqualTo("/shorts/bbbbbbbbbbb")
            assertThat(result).isEqualTo(AppResult.Success(true))
        }

    @Test
    fun `shorts url redirected to watch page is not a short`() =
        runTest {
            enqueue(code = 303, headers = arrayOf("Location" to "https://www.youtube.com/watch?v=aaaaaaaaaaa"))

            assertThat(dataSource.isShortsUrl("aaaaaaaaaaa")).isEqualTo(AppResult.Success(false))
        }

    @Test
    fun `redirect elsewhere is not conclusive`() =
        runTest {
            enqueue(code = 302, headers = arrayOf("Location" to "https://consent.youtube.com/m?continue=x"))

            assertThat(dataSource.isShortsUrl("aaaaaaaaaaa")).isEqualTo(AppResult.Failure(DataError.Http(302)))
        }

    private companion object {
        const val API_KEY = "test-api-key"
    }
}
