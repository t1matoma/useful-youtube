package dev.satoshi.usefulyoutube.core.data.repository

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import dev.satoshi.usefulyoutube.core.data.testdoubles.FakeYouTubeNetworkDataSource.Companion.details
import dev.satoshi.usefulyoutube.core.data.testdoubles.FakeYouTubeNetworkDataSource.Companion.searchPage
import dev.satoshi.usefulyoutube.core.domain.repository.SearchPage
import dev.satoshi.usefulyoutube.core.domain.repository.SearchSession
import dev.satoshi.usefulyoutube.core.model.AppResult
import dev.satoshi.usefulyoutube.core.model.ChannelId
import dev.satoshi.usefulyoutube.core.model.DataError
import dev.satoshi.usefulyoutube.core.model.SearchOrder
import dev.satoshi.usefulyoutube.core.model.SearchResults
import dev.satoshi.usefulyoutube.core.model.Video
import dev.satoshi.usefulyoutube.core.model.VideoContentKind
import dev.satoshi.usefulyoutube.core.model.VideoId
import dev.satoshi.usefulyoutube.core.network.VideoSearchRequest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.util.Locale
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class OfflineFirstSearchRepositoryTest : RepositoryTest() {
    private var deviceLocale: Locale = Locale.forLanguageTag("de-DE")
    private val repository =
        OfflineFirstSearchRepository(
            searchDao = database.searchDao(),
            recentQueryDao = database.recentSearchQueryDao(),
            networkDataSource = network,
            locale = { deviceLocale },
            ioDispatcher = testDispatcher,
        )

    private fun video(
        id: String,
        kind: VideoContentKind = VideoContentKind.REGULAR,
    ) = Video(
        id = VideoId(id),
        channelId = ChannelId("UCother"),
        channelTitle = "Other channel",
        title = "Title $id",
        description = "Description",
        publishedAt = Instant.parse("2026-10-30T16:00:06Z"),
        duration = 10.minutes,
        thumbnailUrl = "https://i.ytimg.com/vi/$id/hqdefault.jpg",
        contentKind = kind,
    )

    @Test
    fun `page keeps search order and loads details of found videos`() =
        runTest(testDispatcher) {
            network.searchPages[null] = searchPage("b", "missing", "a", "b", nextPageToken = "page2")
            network.videoDetails["a"] = details("a", "UCa", "PT10M")
            network.videoDetails["b"] = details("b", "UCb", "PT45S")

            val result = repository.fetchPage("kotlin", SearchOrder.DATE, pageToken = null)

            val page = (result as AppResult.Success).value
            assertThat(page.videos.map { it.id.value }).containsExactly("b", "a")
            assertThat(page.videos.map { it.channelTitle }).containsExactly("Channel UCb", "Channel UCa")
            assertThat(page.videos.map { it.duration }).containsExactly(45.seconds, 10.minutes)
            assertThat(page.videos.map { it.contentKind }.toSet()).isEqualTo(setOf(VideoContentKind.UNCLASSIFIED))
            assertThat(page.nextPageToken).isEqualTo("page2")
            assertThat(network.requestedDetailIds).containsExactly(listOf("b", "missing", "a"))
            assertThat(network.searchRequests).containsExactly(
                VideoSearchRequest("kotlin", SearchOrder.DATE, null, regionCode = "DE", relevanceLanguage = "de"),
            )
        }

    @Test
    fun `locale without a country searches without region`() =
        runTest(testDispatcher) {
            deviceLocale = Locale.forLanguageTag("es-419")
            network.searchPages["page2"] = searchPage(nextPageToken = "")

            val result = repository.fetchPage("q", SearchOrder.RELEVANCE, pageToken = "page2")

            assertThat(result).isEqualTo(AppResult.Success(SearchPage(emptyList(), nextPageToken = null)))
            assertThat(network.searchRequests.single().regionCode).isNull()
            assertThat(network.searchRequests.single().relevanceLanguage).isEqualTo("es")
            assertThat(network.requestedDetailIds).isEmpty()
        }

    @Test
    fun `videos with an invalid date are skipped`() =
        runTest(testDispatcher) {
            network.searchPages[null] = searchPage("a")
            network.videoDetails["a"] = details("a", "UCa", "PT10M", publishedAt = "yesterday")

            assertThat(repository.fetchPage("q", SearchOrder.RELEVANCE, null))
                .isEqualTo(AppResult.Success(SearchPage(emptyList(), nextPageToken = null)))
        }

    @Test
    fun `search and details failures are returned`() =
        runTest(testDispatcher) {
            network.searchPages[null] = AppResult.Failure(DataError.QuotaExceeded)
            assertThat(repository.fetchPage("q", SearchOrder.RELEVANCE, null))
                .isEqualTo(AppResult.Failure(DataError.QuotaExceeded))

            network.searchPages[null] = searchPage("a")
            network.videoDetailsError = DataError.NoNetwork
            assertThat(repository.fetchPage("q", SearchOrder.RELEVANCE, null))
                .isEqualTo(AppResult.Failure(DataError.NoNetwork))
        }

    @Test
    fun `stored pages are observed as results and single videos`() =
        runTest(testDispatcher) {
            assertThat(repository.observeResults().first()).isNull()

            repository.saveFirstPage(SearchSession("kotlin", SearchOrder.DATE, "page2"), listOf(video("a")))
            repository.saveNextPage(
                nextPageToken = null,
                videos = listOf(video("a"), video("b", VideoContentKind.LIVE_OR_PREMIERE)),
            )

            repository.observeResults().test {
                assertThat(awaitItem()).isEqualTo(
                    SearchResults(
                        query = "kotlin",
                        order = SearchOrder.DATE,
                        videos = listOf(video("a"), video("b", VideoContentKind.LIVE_OR_PREMIERE)),
                        canLoadMore = false,
                    ),
                )
            }
            assertThat(repository.getSession()).isEqualTo(SearchSession("kotlin", SearchOrder.DATE, null))
            assertThat(
                repository.observeVideo(VideoId("b")).first(),
            ).isEqualTo(video("b", VideoContentKind.LIVE_OR_PREMIERE))
        }

    @Test
    fun `recent queries are saved, limited and deleted`() =
        runTest(testDispatcher) {
            (1..12).forEach { repository.saveRecentQuery("query $it") }
            repository.deleteRecentQuery("query 12")

            val queries = repository.observeRecentQueries().first()

            assertThat(queries.size).isEqualTo(9)
            assertThat(queries.first()).isEqualTo("query 11")
        }
}
