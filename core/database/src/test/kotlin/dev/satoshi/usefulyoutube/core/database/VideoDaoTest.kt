package dev.satoshi.usefulyoutube.core.database

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import dev.satoshi.usefulyoutube.core.database.entity.UnclassifiedVideo
import dev.satoshi.usefulyoutube.core.database.entity.VideoContentKindUpdate
import dev.satoshi.usefulyoutube.core.database.entity.VideoDetailsUpdate
import dev.satoshi.usefulyoutube.core.model.VideoContentKind
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class VideoDaoTest : DatabaseTest() {
    private val videoDao get() = database.videoDao()

    @Before
    fun insertChannels() =
        runTest {
            database.channelDao().insertOrIgnore(listOf(channel("a", "First"), channel("b", "Second")))
        }

    @Test
    fun `feed contains requested kinds sorted by date with channel title`() =
        runTest {
            videoDao.insertOrIgnore(
                listOf(
                    video("old", "a", publishedAtEpochMillis = 1),
                    video("short", "a", publishedAtEpochMillis = 2, contentKind = VideoContentKind.SHORT),
                    video("live", "b", publishedAtEpochMillis = 3, contentKind = VideoContentKind.LIVE_OR_PREMIERE),
                    video("new", "b", publishedAtEpochMillis = 4),
                    video("pending", "b", publishedAtEpochMillis = 5, contentKind = VideoContentKind.UNCLASSIFIED),
                ),
            )

            videoDao.observeVideos(setOf(VideoContentKind.REGULAR, VideoContentKind.LIVE_OR_PREMIERE)).test {
                val feed = awaitItem()
                assertThat(feed.map { it.video.id }).containsExactly("new", "live", "old")
                assertThat(feed.map { it.channelTitle }).containsExactly("Second", "Second", "First")
            }
        }

    @Test
    fun `existing videos are not overwritten`() =
        runTest {
            videoDao.insertOrIgnore(listOf(video("v", "a", contentKind = VideoContentKind.SHORT)))

            val rowIds = videoDao.insertOrIgnore(listOf(video("v", "a", contentKind = VideoContentKind.UNCLASSIFIED)))

            assertThat(rowIds).containsExactly(-1L)
            videoDao.observeVideo("v").test {
                assertThat(awaitItem()?.video?.contentKind).isEqualTo(VideoContentKind.SHORT)
            }
        }

    @Test
    fun `details update marks video as fetched`() =
        runTest {
            videoDao.insertOrIgnore(
                listOf(
                    video("fetched", "a"),
                    video("pending", "a", detailsFetched = false, contentKind = VideoContentKind.UNCLASSIFIED),
                ),
            )
            assertThat(videoDao.getIdsWithoutDetails()).containsExactly("pending")

            videoDao.updateDetails(
                listOf(VideoDetailsUpdate("pending", "New title", "Description", 42, "https://thumbnail")),
            )

            assertThat(videoDao.getIdsWithoutDetails()).containsExactly()
            assertThat(videoDao.getUnclassifiedVideos()).containsExactly(UnclassifiedVideo("pending", 42))
            videoDao.observeVideo("pending").test {
                val video = awaitItem()?.video
                assertThat(video?.title).isEqualTo("New title")
                assertThat(video?.durationSeconds).isEqualTo(42L)
            }
        }

    @Test
    fun `content kinds are updated`() =
        runTest {
            videoDao.insertOrIgnore(listOf(video("v", "a", contentKind = VideoContentKind.UNCLASSIFIED)))

            videoDao.updateContentKinds(listOf(VideoContentKindUpdate("v", VideoContentKind.SHORT)))

            assertThat(videoDao.getUnclassifiedVideos()).containsExactly()
            videoDao.observeVideo("v").test {
                assertThat(awaitItem()?.video?.contentKind).isEqualTo(VideoContentKind.SHORT)
            }
        }

    @Test
    fun `missing video is null`() =
        runTest {
            videoDao.observeVideo("missing").test { assertThat(awaitItem()).isNull() }
        }
}
