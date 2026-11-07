package dev.satoshi.usefulyoutube.core.domain.usecase

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import dev.satoshi.usefulyoutube.core.model.VideoContentKind
import dev.satoshi.usefulyoutube.core.model.VideoId
import dev.satoshi.usefulyoutube.core.testing.data.TestData
import dev.satoshi.usefulyoutube.core.testing.repository.FakeChannelRepository
import dev.satoshi.usefulyoutube.core.testing.repository.FakeHistoryRepository
import dev.satoshi.usefulyoutube.core.testing.repository.FakePlaylistRepository
import dev.satoshi.usefulyoutube.core.testing.repository.FakeSearchRepository
import dev.satoshi.usefulyoutube.core.testing.repository.FakeVideoRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.time.Instant

class ObservationUseCasesTest {
    private val older = TestData.video(id = "older", publishedAt = Instant.parse("2026-09-01T00:00:00Z"))
    private val newer = TestData.video(id = "newer", publishedAt = Instant.parse("2026-10-01T00:00:00Z"))
    private val short = TestData.video(id = "short", contentKind = VideoContentKind.SHORT)
    private val videoRepository = FakeVideoRepository(listOf(older, short, newer))
    private val channelRepository = FakeChannelRepository(listOf(TestData.firstChannel, TestData.secondChannel))

    @Test
    fun `feed contains videos without shorts, newest first`() =
        runTest {
            ObserveFeedUseCase(videoRepository)().test {
                assertThat(awaitItem()).containsExactly(newer, older)
            }
        }

    @Test
    fun `video is observed by id`() =
        runTest {
            val observeVideo =
                ObserveVideoUseCase(
                    videoRepository,
                    FakeSearchRepository(),
                    FakePlaylistRepository(),
                    FakeHistoryRepository(),
                )

            observeVideo(older.id).test { assertThat(awaitItem()).isEqualTo(older) }
            observeVideo(VideoId("missing")).test { assertThat(awaitItem()).isNull() }
        }

    @Test
    fun `channels are observed and removed`() =
        runTest {
            ObserveChannelsUseCase(channelRepository)().test {
                assertThat(awaitItem()).containsExactly(TestData.firstChannel, TestData.secondChannel)

                RemoveChannelUseCase(channelRepository)(TestData.firstChannel.id)
                assertThat(awaitItem()).containsExactly(TestData.secondChannel)

                RemoveChannelUseCase(channelRepository)(TestData.secondChannel.id)
                assertThat(awaitItem()).isEmpty()
            }
        }
}
