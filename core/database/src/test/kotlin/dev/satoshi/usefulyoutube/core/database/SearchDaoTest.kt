package dev.satoshi.usefulyoutube.core.database

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import dev.satoshi.usefulyoutube.core.database.entity.SearchResultEntity
import dev.satoshi.usefulyoutube.core.database.entity.SearchSessionEntity
import dev.satoshi.usefulyoutube.core.model.SearchOrder
import dev.satoshi.usefulyoutube.core.model.VideoContentKind
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SearchDaoTest : DatabaseTest() {
    private val searchDao get() = database.searchDao()
    private val recentQueryDao get() = database.recentSearchQueryDao()

    private fun result(videoId: String) =
        SearchResultEntity(
            videoId = videoId,
            position = 0,
            channelId = "UCzzzzzzzzzzzzzzzzzzzzzz",
            channelTitle = "Not subscribed",
            title = "Video $videoId",
            description = "",
            publishedAtEpochMillis = 0,
            durationSeconds = 600,
            thumbnailUrl = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg",
            contentKind = VideoContentKind.REGULAR,
        )

    @Test
    fun `new search replaces the previous one`() =
        runTest {
            searchDao.replaceSearch(SearchSessionEntity("old", SearchOrder.RELEVANCE, "token"), listOf(result("a")))

            searchDao.replaceSearch(
                SearchSessionEntity("new", SearchOrder.DATE, nextPageToken = null),
                listOf(result("c"), result("b")),
            )

            assertThat(searchDao.observeSession().first()).isEqualTo(SearchSessionEntity("new", SearchOrder.DATE, null))
            searchDao.observeResults().test {
                val results = awaitItem()
                assertThat(results.map { it.videoId }).containsExactly("c", "b")
                assertThat(results.map { it.position }).containsExactly(0, 1)
            }
        }

    @Test
    fun `next page is appended after stored results without duplicates`() =
        runTest {
            searchDao.replaceSearch(
                SearchSessionEntity("query", SearchOrder.RELEVANCE, "page2"),
                listOf(result("a"), result("b")),
            )

            searchDao.appendPage(nextPageToken = null, results = listOf(result("b"), result("c")))

            assertThat(searchDao.observeResults().first().map { it.videoId }).containsExactly("a", "b", "c")
            assertThat(searchDao.observeSession().first()?.nextPageToken).isNull()
        }

    @Test
    fun `stored result is observed by video id`() =
        runTest {
            searchDao.replaceSearch(SearchSessionEntity("query", SearchOrder.RELEVANCE, null), listOf(result("a")))

            assertThat(searchDao.observeResult("a").first()?.channelTitle).isEqualTo("Not subscribed")
            assertThat(searchDao.observeResult("missing").first()).isNull()
        }

    @Test
    fun `recent queries are most recent first and limited`() =
        runTest {
            listOf("one", "two", "three", "one").forEach { recentQueryDao.saveRecentQuery(it, limit = 2) }

            assertThat(recentQueryDao.observeRecentQueries().first()).containsExactly("one", "three")
        }

    @Test
    fun `recent query can be deleted`() =
        runTest {
            recentQueryDao.saveRecentQuery("one", limit = 10)
            recentQueryDao.saveRecentQuery("two", limit = 10)

            recentQueryDao.deleteRecentQuery("one")

            assertThat(recentQueryDao.observeRecentQueries().first()).containsExactly("two")
        }
}
