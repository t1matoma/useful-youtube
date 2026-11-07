package dev.satoshi.usefulyoutube.core.domain.usecase

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import dev.satoshi.usefulyoutube.core.domain.repository.SearchPage
import dev.satoshi.usefulyoutube.core.domain.repository.SearchSession
import dev.satoshi.usefulyoutube.core.domain.shorts.DurationShortsDetector
import dev.satoshi.usefulyoutube.core.model.AppResult
import dev.satoshi.usefulyoutube.core.model.DataError
import dev.satoshi.usefulyoutube.core.model.SearchOrder
import dev.satoshi.usefulyoutube.core.model.SearchResults
import dev.satoshi.usefulyoutube.core.model.VideoContentKind
import dev.satoshi.usefulyoutube.core.model.VideoId
import dev.satoshi.usefulyoutube.core.testing.data.TestData
import dev.satoshi.usefulyoutube.core.testing.repository.FakePlaylistRepository
import dev.satoshi.usefulyoutube.core.testing.repository.FakeSearchRepository
import dev.satoshi.usefulyoutube.core.testing.repository.FakeVideoRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class SearchUseCasesTest {
    private val repository = FakeSearchRepository()
    private val classifyVideo = ClassifyVideoUseCase(DurationShortsDetector())
    private val searchVideos = SearchVideosUseCase(repository, classifyVideo)
    private val loadMore = LoadMoreSearchResultsUseCase(repository, classifyVideo)

    private fun found(
        id: String,
        duration: Duration?,
    ) = TestData.video(id = id, duration = duration, contentKind = VideoContentKind.UNCLASSIFIED)

    @Test
    fun `search stores watchable videos of the first page and the session`() =
        runTest {
            repository.remotePages["kotlin" to null] =
                SearchPage(
                    videos =
                        listOf(
                            found("regular", 12.minutes),
                            found("short", 40.seconds),
                            found("live", Duration.ZERO),
                            found("no-details", duration = null),
                        ),
                    nextPageToken = "page2",
                )

            val result = searchVideos("  kotlin ", SearchOrder.DATE)

            assertThat(result).isEqualTo(AppResult.Success(Unit))
            assertThat(repository.fetchedPages).containsExactly(Triple("kotlin", SearchOrder.DATE, null))
            assertThat(repository.currentResults.associate { it.id.value to it.contentKind }).isEqualTo(
                mapOf("regular" to VideoContentKind.REGULAR, "live" to VideoContentKind.LIVE_OR_PREMIERE),
            )
            assertThat(repository.currentSession).isEqualTo(SearchSession("kotlin", SearchOrder.DATE, "page2"))
            assertThat(repository.currentRecentQueries).containsExactly("kotlin")
        }

    @Test
    fun `blank query does not search`() =
        runTest {
            assertThat(searchVideos("   ", SearchOrder.RELEVANCE)).isEqualTo(AppResult.Success(Unit))
            assertThat(repository.fetchedPages).isEmpty()
            assertThat(repository.currentRecentQueries).isEmpty()
        }

    @Test
    fun `failed search keeps previous results and remembers the query`() =
        runTest {
            repository.saveFirstPage(SearchSession("old", SearchOrder.RELEVANCE, null), listOf(TestData.video("old")))
            repository.fetchError = DataError.QuotaExceeded

            val result = searchVideos("new", SearchOrder.RELEVANCE)

            assertThat(result).isEqualTo(AppResult.Failure(DataError.QuotaExceeded))
            assertThat(repository.currentResults.map { it.id.value }).containsExactly("old")
            assertThat(repository.currentRecentQueries).containsExactly("new")
        }

    @Test
    fun `next page is appended with the stored query and order`() =
        runTest {
            repository.saveFirstPage(
                SearchSession("kotlin", SearchOrder.DATE, "page2"),
                listOf(TestData.video("first")),
            )
            repository.remotePages["kotlin" to "page2"] =
                SearchPage(listOf(found("second", 10.minutes), found("short", 30.seconds)), nextPageToken = null)

            val result = loadMore()

            assertThat(result).isEqualTo(AppResult.Success(Unit))
            assertThat(repository.fetchedPages).containsExactly(Triple("kotlin", SearchOrder.DATE, "page2"))
            assertThat(repository.currentResults.map { it.id.value }).containsExactly("first", "second")
            assertThat(repository.currentSession?.nextPageToken).isNull()
        }

    @Test
    fun `nothing is loaded without a next page`() =
        runTest {
            repository.saveFirstPage(SearchSession("kotlin", SearchOrder.DATE, null), emptyList())

            assertThat(loadMore()).isEqualTo(AppResult.Success(Unit))
            assertThat(FakeSearchRepository().let { LoadMoreSearchResultsUseCase(it, classifyVideo)() })
                .isEqualTo(AppResult.Success(Unit))
            assertThat(repository.fetchedPages).isEmpty()
        }

    @Test
    fun `failed next page keeps the token for a retry`() =
        runTest {
            repository.saveFirstPage(SearchSession("kotlin", SearchOrder.DATE, "page2"), emptyList())
            repository.fetchError = DataError.NoNetwork

            assertThat(loadMore()).isEqualTo(AppResult.Failure(DataError.NoNetwork))
            assertThat(repository.currentSession?.nextPageToken).isEqualTo("page2")
        }

    @Test
    fun `results and recent queries are observed and queries deleted`() =
        runTest {
            repository.saveFirstPage(SearchSession("kotlin", SearchOrder.DATE, "page2"), listOf(TestData.video("v")))
            repository.saveRecentQuery("kotlin")
            repository.saveRecentQuery("compose")

            ObserveSearchResultsUseCase(repository)().test {
                assertThat(awaitItem())
                    .isEqualTo(
                        SearchResults("kotlin", SearchOrder.DATE, listOf(TestData.video("v")), canLoadMore = true),
                    )
            }
            ObserveRecentSearchQueriesUseCase(repository)().test {
                assertThat(awaitItem()).containsExactly("compose", "kotlin")
                DeleteRecentSearchQueryUseCase(repository)("kotlin")
                assertThat(awaitItem()).containsExactly("compose")
            }
        }

    @Test
    fun `player finds feed videos first and search results otherwise`() =
        runTest {
            val feedVideo = TestData.video("shared")
            val foundVersion = feedVideo.copy(title = "From search")
            val otherChannelVideo = TestData.video("other")
            repository.saveFirstPage(
                SearchSession("q", SearchOrder.RELEVANCE, null),
                listOf(foundVersion, otherChannelVideo),
            )
            val observeVideo =
                ObserveVideoUseCase(FakeVideoRepository(listOf(feedVideo)), repository, FakePlaylistRepository())

            observeVideo(VideoId("shared")).test { assertThat(awaitItem()).isEqualTo(feedVideo) }
            observeVideo(VideoId("other")).test { assertThat(awaitItem()).isEqualTo(otherChannelVideo) }
            observeVideo(VideoId("missing")).test { assertThat(awaitItem()).isNull() }
        }
}
