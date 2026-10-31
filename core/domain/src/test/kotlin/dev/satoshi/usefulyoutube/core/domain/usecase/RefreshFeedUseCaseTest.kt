package dev.satoshi.usefulyoutube.core.domain.usecase

import assertk.assertThat
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import dev.satoshi.usefulyoutube.core.domain.shorts.DurationShortsDetector
import dev.satoshi.usefulyoutube.core.model.AppResult
import dev.satoshi.usefulyoutube.core.model.DataError
import dev.satoshi.usefulyoutube.core.model.RefreshSummary
import dev.satoshi.usefulyoutube.core.model.VideoContentKind
import dev.satoshi.usefulyoutube.core.testing.data.TestData
import dev.satoshi.usefulyoutube.core.testing.repository.FakeChannelRepository
import dev.satoshi.usefulyoutube.core.testing.repository.FakeVideoRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class RefreshFeedUseCaseTest {
    private val channelRepository = FakeChannelRepository(listOf(TestData.firstChannel, TestData.secondChannel))
    private val videoRepository = FakeVideoRepository()
    private val refreshFeed =
        RefreshFeedUseCase(channelRepository, videoRepository, ClassifyVideoUseCase(DurationShortsDetector()))

    private fun unclassified(
        id: String,
        duration: Duration?,
    ) = TestData.video(id = id, duration = duration, contentKind = VideoContentKind.UNCLASSIFIED)

    @Test
    fun `syncs all channels and classifies new videos`() =
        runTest {
            videoRepository.remoteFeeds[TestData.firstChannel.id] =
                listOf(unclassified("regular", 12.minutes), unclassified("short", 40.seconds))
            videoRepository.remoteFeeds[TestData.secondChannel.id] = listOf(unclassified("live", Duration.ZERO))

            val result = refreshFeed()

            assertThat(
                result,
            ).isEqualTo(AppResult.Success(RefreshSummary(newVideoCount = 3, failedChannels = emptyList())))
            assertThat(videoRepository.currentVideos.associate { it.id.value to it.contentKind }).isEqualTo(
                mapOf(
                    "regular" to VideoContentKind.REGULAR,
                    "short" to VideoContentKind.SHORT,
                    "live" to VideoContentKind.LIVE_OR_PREMIERE,
                ),
            )
        }

    @Test
    fun `videos without details stay unclassified`() =
        runTest {
            videoRepository.remoteFeeds[TestData.firstChannel.id] = listOf(unclassified("pending", null))

            refreshFeed()

            assertThat(videoRepository.currentVideos.single().contentKind).isEqualTo(VideoContentKind.UNCLASSIFIED)
        }

    @Test
    fun `failure of one channel is reported but does not stop refresh`() =
        runTest {
            videoRepository.failingChannels[TestData.firstChannel.id] = DataError.Http(code = 500)
            videoRepository.remoteFeeds[TestData.secondChannel.id] = listOf(unclassified("regular", 12.minutes))

            val result = refreshFeed()

            assertThat(result).isEqualTo(
                AppResult.Success(RefreshSummary(newVideoCount = 1, failedChannels = listOf(TestData.firstChannel.id))),
            )
        }

    @Test
    fun `failure of all channels is an error`() =
        runTest {
            videoRepository.failingChannels[TestData.firstChannel.id] = DataError.NoNetwork
            videoRepository.failingChannels[TestData.secondChannel.id] = DataError.NoNetwork

            assertThat(refreshFeed()).isEqualTo(AppResult.Failure(DataError.NoNetwork))
        }

    @Test
    fun `details failure is an error`() =
        runTest {
            videoRepository.detailsError = DataError.InvalidApiKey

            assertThat(refreshFeed()).isEqualTo(AppResult.Failure(DataError.InvalidApiKey))
        }

    @Test
    fun `refresh without subscriptions succeeds with empty summary`() =
        runTest {
            channelRepository.removeChannel(TestData.firstChannel.id)
            channelRepository.removeChannel(TestData.secondChannel.id)

            assertThat(refreshFeed()).isEqualTo(AppResult.Success(RefreshSummary(0, emptyList())))
            assertThat(videoRepository.syncedChannels).isEmpty()
        }

    @Test
    fun `every channel is synced`() =
        runTest {
            refreshFeed()

            assertThat(videoRepository.syncedChannels).containsExactlyInAnyOrder(
                TestData.firstChannel.id,
                TestData.secondChannel.id,
            )
        }
}
