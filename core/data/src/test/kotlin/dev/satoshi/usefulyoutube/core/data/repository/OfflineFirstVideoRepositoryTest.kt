package dev.satoshi.usefulyoutube.core.data.repository

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import dev.satoshi.usefulyoutube.core.data.testdoubles.FakeYouTubeNetworkDataSource.Companion.details
import dev.satoshi.usefulyoutube.core.data.testdoubles.FakeYouTubeNetworkDataSource.Companion.entry
import dev.satoshi.usefulyoutube.core.data.testdoubles.FakeYouTubeNetworkDataSource.Companion.feed
import dev.satoshi.usefulyoutube.core.database.entity.ChannelEntity
import dev.satoshi.usefulyoutube.core.domain.shorts.ShortsCandidate
import dev.satoshi.usefulyoutube.core.model.AppResult
import dev.satoshi.usefulyoutube.core.model.ChannelId
import dev.satoshi.usefulyoutube.core.model.DataError
import dev.satoshi.usefulyoutube.core.model.VideoContentKind
import dev.satoshi.usefulyoutube.core.model.VideoId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class OfflineFirstVideoRepositoryTest : RepositoryTest() {
    private val repository = OfflineFirstVideoRepository(database.videoDao(), network, testDispatcher)
    private val channelId = ChannelId("UCchannel")

    @Before
    fun insertChannel() =
        runTest(testDispatcher) {
            database.channelDao().insertOrIgnore(listOf(ChannelEntity(channelId.value, "Channel title")))
        }

    @Test
    fun `sync stores new feed entries once`() =
        runTest(testDispatcher) {
            network.feeds[channelId.value] = feed("Channel", entry("v1", channelId.value), entry("v2", channelId.value))

            assertThat(repository.syncChannelFeed(channelId)).isEqualTo(AppResult.Success(2))
            assertThat(repository.syncChannelFeed(channelId)).isEqualTo(AppResult.Success(0))
        }

    @Test
    fun `entries with invalid dates are skipped`() =
        runTest(testDispatcher) {
            network.feeds[channelId.value] =
                feed("Channel", entry("v1", channelId.value, published = "yesterday"), entry("v2", channelId.value))

            assertThat(repository.syncChannelFeed(channelId)).isEqualTo(AppResult.Success(1))
        }

    @Test
    fun `entries whose channel id differs from the feed's channel are stored under the feed's channel`() =
        runTest(testDispatcher) {
            // Real feeds can carry an id that does not match the subscription, for example without "UC".
            network.feeds[channelId.value] =
                feed("Channel", entry("v1", channelId.value.removePrefix("UC")), entry("v2", "UCsomebodyElse"))

            assertThat(repository.syncChannelFeed(channelId)).isEqualTo(AppResult.Success(2))
            assertThat(repository.observeVideo(VideoId("v1")).first()?.channelId).isEqualTo(channelId)
            assertThat(repository.observeVideo(VideoId("v2")).first()?.channelTitle).isEqualTo("Channel title")
        }

    @Test
    fun `feed of a channel removed during the refresh is ignored`() =
        runTest(testDispatcher) {
            val removed = ChannelId("UCremoved")
            network.feeds[removed.value] = feed("Removed", entry("v1", removed.value))

            assertThat(repository.syncChannelFeed(removed)).isEqualTo(AppResult.Success(0))
        }

    @Test
    fun `sync failure is returned`() =
        runTest(testDispatcher) {
            network.feeds[channelId.value] = AppResult.Failure(DataError.NoNetwork)

            assertThat(repository.syncChannelFeed(channelId)).isEqualTo(AppResult.Failure(DataError.NoNetwork))
        }

    @Test
    fun `new videos are hidden from feed until classified`() =
        runTest(testDispatcher) {
            network.feeds[channelId.value] = feed("Channel", entry("v1", channelId.value))
            repository.syncChannelFeed(channelId)

            repository.observeFeed().test {
                assertThat(awaitItem()).isEmpty()

                repository.saveContentKinds(mapOf(VideoId("v1") to VideoContentKind.REGULAR))
                val video = awaitItem().single()
                assertThat(video.id).isEqualTo(VideoId("v1"))
                assertThat(video.channelTitle).isEqualTo("Channel title")
                assertThat(video.title).isEqualTo("RSS title v1")
                assertThat(video.publishedAt).isEqualTo(Instant.parse("2026-10-30T16:00:06Z"))
                assertThat(video.thumbnailUrl).isEqualTo("https://i1.ytimg.com/vi/v1/hqdefault.jpg")
            }
        }

    @Test
    fun `details are fetched only for videos without them`() =
        runTest(testDispatcher) {
            network.feeds[channelId.value] = feed("Channel", entry("v1", channelId.value), entry("v2", channelId.value))
            network.videoDetails["v1"] = details("v1", channelId.value, duration = "PT2M30S")
            network.videoDetails["v2"] = details("v2", channelId.value, duration = "PT1H")
            repository.syncChannelFeed(channelId)

            assertThat(repository.fetchMissingDetails()).isEqualTo(AppResult.Success(Unit))
            assertThat(repository.fetchMissingDetails()).isEqualTo(AppResult.Success(Unit))

            assertThat(network.requestedDetailIds).containsExactly(listOf("v1", "v2"))
            assertThat(repository.getUnclassifiedVideos()).containsExactly(
                ShortsCandidate(VideoId("v1"), 150.seconds),
                ShortsCandidate(VideoId("v2"), 60.minutes),
            )
        }

    @Test
    fun `details replace rss title and description`() =
        runTest(testDispatcher) {
            network.feeds[channelId.value] = feed("Channel", entry("v1", channelId.value))
            network.videoDetails["v1"] = details("v1", channelId.value, duration = "PT10M")
            repository.syncChannelFeed(channelId)
            repository.fetchMissingDetails()
            repository.saveContentKinds(mapOf(VideoId("v1") to VideoContentKind.REGULAR))

            repository.observeVideo(VideoId("v1")).test {
                val video = awaitItem()
                assertThat(video?.title).isEqualTo("API title v1")
                assertThat(video?.description).isEqualTo("API description")
                assertThat(video?.duration).isEqualTo(10.minutes)
            }
        }

    @Test
    fun `details failure is returned`() =
        runTest(testDispatcher) {
            network.feeds[channelId.value] = feed("Channel", entry("v1", channelId.value))
            network.videoDetailsError = DataError.InvalidApiKey
            repository.syncChannelFeed(channelId)

            assertThat(repository.fetchMissingDetails()).isEqualTo(AppResult.Failure(DataError.InvalidApiKey))
        }

    @Test
    fun `no request without pending videos`() =
        runTest(testDispatcher) {
            assertThat(repository.fetchMissingDetails()).isEqualTo(AppResult.Success(Unit))
            assertThat(network.requestedDetailIds).isEmpty()
        }

    @Test
    fun `missing video is null`() =
        runTest(testDispatcher) {
            repository.observeVideo(VideoId("missing")).test { assertThat(awaitItem()).isNull() }
        }
}
