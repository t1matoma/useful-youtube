package dev.satoshi.usefulyoutube.core.network

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import dev.satoshi.usefulyoutube.core.model.AppResult
import dev.satoshi.usefulyoutube.core.model.DataError
import dev.satoshi.usefulyoutube.core.network.api.ApiKeyInterceptor
import dev.satoshi.usefulyoutube.core.network.api.YouTubeDataApi
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

class AccountAndChannelDataSourcesTest {
    private val server = MockWebServer()
    private lateinit var ratings: RetrofitVideoRatingNetworkDataSource
    private lateinit var channelSections: RetrofitChannelSectionsNetworkDataSource

    @Before
    fun setUp() {
        server.start()
        val json = Json { ignoreUnknownKeys = true }
        val api =
            Retrofit
                .Builder()
                .baseUrl(server.url("/youtube/v3/"))
                .client(OkHttpClient().newBuilder().addInterceptor(ApiKeyInterceptor(API_KEY)).build())
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(YouTubeDataApi::class.java)
        ratings = RetrofitVideoRatingNetworkDataSource(api, json)
        channelSections = RetrofitChannelSectionsNetworkDataSource(api, json)
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun enqueue(
        code: Int,
        body: String = "",
    ) = server.enqueue(
        MockResponse
            .Builder()
            .code(code)
            .body(body)
            .build(),
    )

    @Test
    fun `rating of a video is read with bearer token`() =
        runTest {
            enqueue(code = 200, body = Fixtures.read("video_rating_response.json"))

            val result = ratings.getVideoRating(accessToken = "access-token", videoId = "dQw4w9WgXcQ")

            val request = server.takeRequest()
            assertThat(request.method).isEqualTo("GET")
            assertThat(request.url.encodedPath).isEqualTo("/youtube/v3/videos/getRating")
            assertThat(request.url.queryParameter("id")).isEqualTo("dQw4w9WgXcQ")
            assertThat(request.url.queryParameter("key")).isEqualTo(API_KEY)
            assertThat(request.headers["Authorization"]).isEqualTo("Bearer access-token")
            assertThat(result).isEqualTo(AppResult.Success("like"))
        }

    @Test
    fun `a video missing in the rating response is not rated`() =
        runTest {
            enqueue(code = 200, body = """{ "items": [] }""")

            assertThat(ratings.getVideoRating("access-token", "dQw4w9WgXcQ")).isEqualTo(AppResult.Success("none"))
        }

    @Test
    fun `rating is posted and the empty answer is a success`() =
        runTest {
            enqueue(code = 204)

            val result = ratings.rateVideo(accessToken = "access-token", videoId = "dQw4w9WgXcQ", rating = "dislike")

            val request = server.takeRequest()
            assertThat(request.method).isEqualTo("POST")
            assertThat(request.url.encodedPath).isEqualTo("/youtube/v3/videos/rate")
            assertThat(request.url.queryParameter("id")).isEqualTo("dQw4w9WgXcQ")
            assertThat(request.url.queryParameter("rating")).isEqualTo("dislike")
            assertThat(request.headers["Authorization"]).isEqualTo("Bearer access-token")
            assertThat(result).isEqualTo(AppResult.Success(Unit))
        }

    @Test
    fun `rejected rating reports the api error`() =
        runTest {
            enqueue(code = 401, body = Fixtures.read("error_auth.json"))
            enqueue(code = 403, body = Fixtures.read("error_quota_exceeded.json"))

            assertThat(ratings.rateVideo("expired", "dQw4w9WgXcQ", "like"))
                .isEqualTo(AppResult.Failure(DataError.Unauthorized))
            assertThat(ratings.rateVideo("access-token", "dQw4w9WgXcQ", "like"))
                .isEqualTo(AppResult.Failure(DataError.QuotaExceeded))
        }

    @Test
    fun `featured channels come from the multiple channels sections without duplicates`() =
        runTest {
            enqueue(code = 200, body = Fixtures.read("channel_sections_response.json"))

            val result = channelSections.getFeaturedChannelIds("UCBJycsmduvYEL83R_U4JriQ")

            val request = server.takeRequest()
            assertThat(request.url.encodedPath).isEqualTo("/youtube/v3/channelSections")
            assertThat(request.url.queryParameter("channelId")).isEqualTo("UCBJycsmduvYEL83R_U4JriQ")
            assertThat(request.url.queryParameter("part")).isEqualTo("snippet,contentDetails")
            assertThat((result as AppResult.Success).value).containsExactly(
                "UCG7J20LhUeLl6y_Emi7OJrA",
                "UC2J-0g_nxlwcD9JBK1eTleQ",
                "UCEcrRXW3oEYfUctetZTAWLw",
                "UCq8ZAAsI89IoJ-fn1gYpO3g",
            )
        }

    @Test
    fun `a channel without sections features nobody`() =
        runTest {
            enqueue(code = 200, body = """{ "kind": "youtube#channelSectionListResponse" }""")

            assertThat((channelSections.getFeaturedChannelIds("UCaaa") as AppResult.Success).value).isEmpty()
        }

    private companion object {
        const val API_KEY = "test-key"
    }
}
