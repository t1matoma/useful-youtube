package dev.satoshi.usefulyoutube.core.domain.usecase

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import dev.satoshi.usefulyoutube.core.domain.recommendations.RecommendationRanker
import dev.satoshi.usefulyoutube.core.domain.shorts.DurationShortsDetector
import dev.satoshi.usefulyoutube.core.model.AppResult
import dev.satoshi.usefulyoutube.core.model.Channel
import dev.satoshi.usefulyoutube.core.model.ChannelId
import dev.satoshi.usefulyoutube.core.model.DataError
import dev.satoshi.usefulyoutube.core.model.DiscoveredVideo
import dev.satoshi.usefulyoutube.core.model.Video
import dev.satoshi.usefulyoutube.core.model.VideoContentKind
import dev.satoshi.usefulyoutube.core.model.VideoRating
import dev.satoshi.usefulyoutube.core.testing.data.TestData
import dev.satoshi.usefulyoutube.core.testing.repository.FakeChannelRepository
import dev.satoshi.usefulyoutube.core.testing.repository.FakeHistoryRepository
import dev.satoshi.usefulyoutube.core.testing.repository.FakeRecommendationRepository
import dev.satoshi.usefulyoutube.core.testing.repository.FakeVideoRatingRepository
import dev.satoshi.usefulyoutube.core.testing.repository.FakeVideoRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class RecommendationUseCasesTest {
    private val favorite = TestData.firstChannel
    private val featured = Channel(ChannelId("UCffffffffffffffffffffff"), "Featured channel")
    private val stranger = Channel(ChannelId("UCssssssssssssssssssssss"), "Watched from the search")
    private val now = Instant.parse("2026-11-10T12:00:00Z")

    private val channels = FakeChannelRepository(listOf(favorite, TestData.secondChannel))
    private val history = FakeHistoryRepository()
    private val ratings = FakeVideoRatingRepository()
    private val recommendations = FakeRecommendationRepository()
    private val refresh =
        RefreshRecommendationsUseCase(
            recommendations,
            channels,
            history,
            ratings,
            ClassifyVideoUseCase(DurationShortsDetector()),
        )

    private fun found(
        id: String,
        channel: Channel,
        duration: Duration? = 10.minutes,
    ) = TestData.video(id = id, channel = channel, duration = duration, contentKind = VideoContentKind.UNCLASSIFIED)

    private fun observe(feed: List<Video> = emptyList()) =
        ObserveRecommendationsUseCase(
            FakeVideoRepository(feed),
            recommendations,
            history,
            ratings,
            RecommendationRanker(),
            object : Clock {
                override fun now() = now
            },
        )

    @Test
    fun `without history and likes nothing is requested`() =
        runTest {
            val result = refresh()

            assertThat(result).isEqualTo(AppResult.Success(Unit))
            assertThat(recommendations.featuredRequests).isEmpty()
            assertThat(recommendations.videoRequests).containsExactly(emptyList<ChannelId>())
            assertThat(recommendations.storedVideos).isEmpty()
        }

    @Test
    fun `videos of channels featured by a favorite channel are stored without unknown kinds`() =
        runTest {
            history.recordWatched(TestData.video("seen", favorite))
            recommendations.featuredChannels[favorite.id] = listOf(featured.id, TestData.secondChannel.id)
            recommendations.remoteVideos[featured.id] =
                listOf(
                    found("long", featured),
                    found("short", featured, 30.seconds),
                    found("live", featured, Duration.ZERO),
                    found("unknown", featured, duration = null),
                )

            val result = refresh()

            assertThat(result).isEqualTo(AppResult.Success(Unit))
            // The subscribed channel is already in the feed.
            assertThat(recommendations.videoRequests).containsExactly(listOf(featured.id))
            assertThat(recommendations.storedVideos).containsExactly(
                DiscoveredVideo(found("long", featured).copy(contentKind = VideoContentKind.REGULAR), favorite.id),
                DiscoveredVideo(
                    found("short", featured, 30.seconds).copy(contentKind = VideoContentKind.SHORT),
                    favorite.id,
                ),
                DiscoveredVideo(
                    found("live", featured, Duration.ZERO).copy(contentKind = VideoContentKind.LIVE_OR_PREMIERE),
                    favorite.id,
                ),
            )
        }

    @Test
    fun `channels watched or liked without a subscription come first and are their own source`() =
        runTest {
            history.recordWatched(TestData.video("seen", favorite))
            ratings.saveRating(TestData.video("liked", stranger), VideoRating.LIKE)
            recommendations.featuredChannels[stranger.id] = listOf(featured.id)
            recommendations.remoteVideos[stranger.id] = listOf(found("more", stranger))
            recommendations.remoteVideos[featured.id] = listOf(found("new", featured))

            refresh()

            assertThat(recommendations.featuredRequests).containsExactly(stranger.id, favorite.id)
            assertThat(recommendations.videoRequests).containsExactly(listOf(stranger.id, featured.id))
            assertThat(recommendations.storedVideos.map { it.video.id.value to it.suggestedBy })
                .containsExactly("more" to null, "new" to stranger.id)
        }

    @Test
    fun `disliked channels are not a source and at most five favorites are asked`() =
        runTest {
            val watchedChannels = (1..7).map { Channel(ChannelId("UC%022d".format(it)), "Channel $it") }
            watchedChannels.forEach { history.recordWatched(TestData.video("seen-${it.id.value}", it)) }
            ratings.saveRating(TestData.video("bad", watchedChannels.first()), VideoRating.DISLIKE)

            refresh()

            assertThat(recommendations.featuredRequests.size).isEqualTo(5)
            assertThat(recommendations.videoRequests.single().toSet())
                .isEqualTo(watchedChannels.drop(1).map { it.id }.toSet())
        }

    @Test
    fun `videos of channels that are not suggested anymore are forgotten`() =
        runTest {
            history.recordWatched(TestData.video("seen", favorite))
            recommendations.featuredChannels[favorite.id] = listOf(featured.id)
            recommendations.remoteVideos[featured.id] = listOf(found("new", featured))
            refresh()

            recommendations.featuredChannels[favorite.id] = emptyList()
            refresh()

            assertThat(recommendations.storedVideos).isEmpty()
        }

    @Test
    fun `failures of the requests are reported and keep the stored videos`() =
        runTest {
            history.recordWatched(TestData.video("seen", favorite))
            recommendations.featuredChannels[favorite.id] = listOf(featured.id)
            recommendations.remoteVideos[featured.id] = listOf(found("new", featured))
            refresh()

            recommendations.featuredError = DataError.QuotaExceeded
            val featuredFailure = refresh()
            recommendations.featuredError = null
            recommendations.videosError = DataError.NoNetwork
            val videosFailure = refresh()

            assertThat(featuredFailure).isEqualTo(AppResult.Failure(DataError.QuotaExceeded))
            assertThat(videosFailure).isEqualTo(AppResult.Failure(DataError.NoNetwork))
            assertThat(recommendations.storedVideos.map { it.video.id.value }).containsExactly("new")
        }

    @Test
    fun `recommendations mix the feed with discovered videos and hide shorts`() =
        runTest {
            history.recordWatched(TestData.video("seen", favorite, publishedAt = now))
            recommendations.featuredChannels[favorite.id] = listOf(featured.id)
            recommendations.remoteVideos[featured.id] =
                listOf(
                    found("new", featured).copy(publishedAt = now),
                    found("short", featured, 30.seconds).copy(publishedAt = now),
                )
            refresh()
            val feed =
                listOf(
                    TestData.video("own", favorite, publishedAt = now),
                    TestData.video("other", TestData.secondChannel, publishedAt = now),
                    TestData.video("seen", favorite, publishedAt = now),
                )

            observe(feed)().test {
                assertThat(awaitItem().map { it.id.value }).containsExactly("own", "new", "other")
            }
        }

    @Test
    fun `player finds discovered videos`() =
        runTest {
            recommendations.saveDiscoveredVideos(
                listOf(DiscoveredVideo(TestData.video("new", featured), null)),
                setOf(featured.id),
            )

            recommendations.observeVideo(TestData.video("new").id).test {
                assertThat(awaitItem()?.channelId).isEqualTo(featured.id)
            }
        }
}
