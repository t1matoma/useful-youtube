package dev.satoshi.usefulyoutube.core.data.repository

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import dev.satoshi.usefulyoutube.core.data.testdoubles.FakeChannelSectionsNetworkDataSource
import dev.satoshi.usefulyoutube.core.data.testdoubles.FakeYouTubeNetworkDataSource.Companion.details
import dev.satoshi.usefulyoutube.core.data.testdoubles.FakeYouTubeNetworkDataSource.Companion.entry
import dev.satoshi.usefulyoutube.core.data.testdoubles.FakeYouTubeNetworkDataSource.Companion.feed
import dev.satoshi.usefulyoutube.core.model.AppResult
import dev.satoshi.usefulyoutube.core.model.ChannelId
import dev.satoshi.usefulyoutube.core.model.DataError
import dev.satoshi.usefulyoutube.core.model.DiscoveredVideo
import dev.satoshi.usefulyoutube.core.model.Video
import dev.satoshi.usefulyoutube.core.model.VideoContentKind
import dev.satoshi.usefulyoutube.core.model.VideoId
import dev.satoshi.usefulyoutube.core.testing.data.TestData
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class OfflineFirstRecommendationRepositoryTest : RepositoryTest() {
    private var now = Instant.parse("2026-11-10T12:00:00Z")
    private val channelSections = FakeChannelSectionsNetworkDataSource()
    private val repository =
        OfflineFirstRecommendationRepository(
            database.discoveryDao(),
            network,
            channelSections,
            object : Clock {
                override fun now() = this@OfflineFirstRecommendationRepositoryTest.now
            },
            testDispatcher,
        )
    private val source = ChannelId("UCsource")
    private val first = ChannelId("UCfirst")
    private val second = ChannelId("UCsecond")

    private fun discovered(
        id: String,
        channelId: ChannelId,
        kind: VideoContentKind = VideoContentKind.REGULAR,
        suggestedBy: ChannelId? = source,
    ) = DiscoveredVideo(
        TestData.video(id = id, contentKind = kind).copy(channelId = channelId, channelTitle = "Found"),
        suggestedBy,
    )

    @Test
    fun `featured channels are requested once and reused for a week`() =
        runTest(testDispatcher) {
            channelSections.featured[source.value] = listOf(first.value, second.value)

            val fresh = repository.getFeaturedChannels(source)
            now += 6.days
            val reused = repository.getFeaturedChannels(source)

            assertThat(fresh).isEqualTo(AppResult.Success(listOf(first, second)))
            assertThat(reused).isEqualTo(fresh)
            assertThat(channelSections.requests).containsExactly(source.value)

            now += 2.days
            channelSections.featured[source.value] = listOf(second.value)

            assertThat(repository.getFeaturedChannels(source)).isEqualTo(AppResult.Success(listOf(second)))
            assertThat(channelSections.requests).hasSize(2)
        }

    @Test
    fun `a channel that features nobody is remembered too`() =
        runTest(testDispatcher) {
            repository.getFeaturedChannels(source)

            assertThat(repository.getFeaturedChannels(source)).isEqualTo(AppResult.Success(emptyList()))
            assertThat(channelSections.requests).hasSize(1)
        }

    @Test
    fun `a failed request falls back to the outdated answer or fails without one`() =
        runTest(testDispatcher) {
            channelSections.error = DataError.QuotaExceeded
            assertThat(repository.getFeaturedChannels(source)).isEqualTo(AppResult.Failure(DataError.QuotaExceeded))

            channelSections.error = null
            channelSections.featured[source.value] = listOf(first.value)
            repository.getFeaturedChannels(source)
            now += 8.days
            channelSections.error = DataError.NoNetwork

            assertThat(repository.getFeaturedChannels(source)).isEqualTo(AppResult.Success(listOf(first)))
        }

    @Test
    fun `new videos get details and belong to the channel whose feed was requested`() =
        runTest(testDispatcher) {
            network.feeds[first.value] = feed("First", entry("a", "first-without-prefix"), entry("b", first.value))
            network.videoDetails["a"] = details("a", "UCelsewhere", duration = "PT10M")
            // "b" is private: the Data API does not return it.

            val result = repository.fetchNewVideos(listOf(first))

            val videos = (result as AppResult.Success<List<Video>>).value
            assertThat(videos.map { it.id.value }).containsExactly("a")
            assertThat(videos.single().channelId).isEqualTo(first)
            assertThat(videos.single().duration).isEqualTo(10.minutes)
            assertThat(videos.single().contentKind).isEqualTo(VideoContentKind.UNCLASSIFIED)
            assertThat(network.requestedDetailIds).containsExactly(listOf("a", "b"))
        }

    @Test
    fun `stored videos are not loaded again and a failed feed is skipped`() =
        runTest(testDispatcher) {
            repository.saveDiscoveredVideos(
                listOf(discovered("a", first), discovered("short", first, VideoContentKind.SHORT)),
                setOf(first),
            )
            network.feeds[first.value] = feed("First", entry("a", first.value), entry("short", first.value))
            network.feeds[second.value] = AppResult.Failure(DataError.Http(500))

            val result = repository.fetchNewVideos(listOf(first, second))

            assertThat(result).isEqualTo(AppResult.Success(emptyList()))
            assertThat(network.requestedDetailIds).isEmpty()
        }

    @Test
    fun `failure of every feed or of the details is reported`() =
        runTest(testDispatcher) {
            network.feeds[first.value] = AppResult.Failure(DataError.NoNetwork)
            assertThat(repository.fetchNewVideos(listOf(first))).isEqualTo(AppResult.Failure(DataError.NoNetwork))

            network.feeds[first.value] = feed("First", entry("a", first.value))
            network.videoDetailsError = DataError.QuotaExceeded
            assertThat(repository.fetchNewVideos(listOf(first))).isEqualTo(AppResult.Failure(DataError.QuotaExceeded))

            assertThat(repository.fetchNewVideos(emptyList())).isEqualTo(AppResult.Success(emptyList()))
        }

    @Test
    fun `discovered videos are observed without shorts and found by id`() =
        runTest(testDispatcher) {
            repository.saveDiscoveredVideos(
                listOf(
                    discovered("regular", first),
                    discovered("short", first, VideoContentKind.SHORT),
                    discovered("own", second, suggestedBy = null),
                ),
                setOf(first, second),
            )

            assertThat(repository.observeDiscoveredVideos().first()).containsExactlyInAnyOrder(
                discovered("regular", first),
                discovered("own", second, suggestedBy = null),
            )
            assertThat(repository.observeVideo(VideoId("short")).first()?.contentKind).isEqualTo(VideoContentKind.SHORT)
            assertThat(repository.observeVideo(VideoId("missing")).first()).isNull()
        }

    @Test
    fun `videos of channels that left the list are forgotten`() =
        runTest(testDispatcher) {
            repository.saveDiscoveredVideos(
                listOf(discovered("a", first), discovered("b", second)),
                setOf(first, second),
            )

            repository.saveDiscoveredVideos(listOf(discovered("c", second)), setOf(second))

            assertThat(repository.observeDiscoveredVideos().first().map { it.video.id.value })
                .containsExactlyInAnyOrder("b", "c")
        }
}
