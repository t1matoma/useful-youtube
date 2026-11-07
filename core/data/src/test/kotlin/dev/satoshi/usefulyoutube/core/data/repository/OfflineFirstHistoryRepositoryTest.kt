package dev.satoshi.usefulyoutube.core.data.repository

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import dev.satoshi.usefulyoutube.core.model.ChannelId
import dev.satoshi.usefulyoutube.core.model.Video
import dev.satoshi.usefulyoutube.core.model.VideoContentKind
import dev.satoshi.usefulyoutube.core.model.VideoId
import dev.satoshi.usefulyoutube.core.model.WatchedVideo
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class OfflineFirstHistoryRepositoryTest : RepositoryTest() {
    private var now = Instant.parse("2026-11-07T12:00:00Z")
    private val repository =
        OfflineFirstHistoryRepository(
            database.watchHistoryDao(),
            object : Clock {
                override fun now() = this@OfflineFirstHistoryRepositoryTest.now
            },
        )

    private fun video(id: String) =
        Video(
            id = VideoId(id),
            channelId = ChannelId("UCother"),
            channelTitle = "Other channel",
            title = "Title $id",
            description = "",
            publishedAt = Instant.parse("2026-10-30T16:00:06Z"),
            duration = 10.minutes,
            thumbnailUrl = "https://i.ytimg.com/vi/$id/hqdefault.jpg",
            contentKind = VideoContentKind.REGULAR,
        )

    @Test
    fun `watched videos are stored with the current time, newest first`() =
        runTest(testDispatcher) {
            val first = now
            repository.recordWatched(video("a"))
            now += 5.minutes
            repository.recordWatched(video("b"))

            assertThat(repository.observeHistory().first()).containsExactly(
                WatchedVideo(video("b"), now),
                WatchedVideo(video("a"), first),
            )
            assertThat(repository.observeVideo(VideoId("a")).first()).isEqualTo(video("a"))
        }

    @Test
    fun `entries are removed and cleared`() =
        runTest(testDispatcher) {
            repository.recordWatched(video("a"))
            now += 1.minutes
            repository.recordWatched(video("b"))

            repository.remove(VideoId("a"))
            assertThat(repository.observeVideo(VideoId("a")).first()).isNull()

            repository.clear()
            assertThat(repository.observeHistory().first()).isEmpty()
        }
}
