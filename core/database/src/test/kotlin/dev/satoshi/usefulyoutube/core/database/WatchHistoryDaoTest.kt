package dev.satoshi.usefulyoutube.core.database

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import dev.satoshi.usefulyoutube.core.database.entity.VideoSnapshot
import dev.satoshi.usefulyoutube.core.database.entity.WatchHistoryEntity
import dev.satoshi.usefulyoutube.core.model.VideoContentKind
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class WatchHistoryDaoTest : DatabaseTest() {
    private val historyDao get() = database.watchHistoryDao()

    private fun entry(
        videoId: String,
        watchedAt: Long,
    ) = WatchHistoryEntity(
        watchedAtEpochMillis = watchedAt,
        video =
            VideoSnapshot(
                videoId = videoId,
                channelId = "UCzzzzzzzzzzzzzzzzzzzzzz",
                channelTitle = "Channel",
                title = "Video $videoId",
                description = "",
                publishedAtEpochMillis = 0,
                durationSeconds = 600,
                thumbnailUrl = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg",
                contentKind = VideoContentKind.REGULAR,
            ),
    )

    @Test
    fun `history is newest first and a video watched again moves to the top`() =
        runTest {
            historyDao.record(entry("a", watchedAt = 1), limit = 10)
            historyDao.record(entry("b", watchedAt = 2), limit = 10)

            historyDao.record(entry("a", watchedAt = 3), limit = 10)

            assertThat(historyDao.observeHistory().first().map { it.video.videoId to it.watchedAtEpochMillis })
                .containsExactly("a" to 3L, "b" to 2L)
        }

    @Test
    fun `only the most recent videos are kept`() =
        runTest {
            (1..5).forEach { historyDao.record(entry("v$it", watchedAt = it.toLong()), limit = 3) }

            assertThat(historyDao.observeHistory().first().map { it.video.videoId }).containsExactly("v5", "v4", "v3")
        }

    @Test
    fun `entries are found, deleted and cleared`() =
        runTest {
            historyDao.record(entry("a", watchedAt = 1), limit = 10)
            historyDao.record(entry("b", watchedAt = 2), limit = 10)

            assertThat(
                historyDao
                    .observeEntry("a")
                    .first()
                    ?.video
                    ?.title,
            ).isEqualTo("Video a")
            historyDao.delete("a")
            assertThat(historyDao.observeEntry("a").first()).isNull()

            historyDao.clear()
            assertThat(historyDao.observeHistory().first()).isEmpty()
        }
}
