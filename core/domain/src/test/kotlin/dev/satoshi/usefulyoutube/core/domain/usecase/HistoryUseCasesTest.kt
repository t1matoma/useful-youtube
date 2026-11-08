package dev.satoshi.usefulyoutube.core.domain.usecase

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.containsOnly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import dev.satoshi.usefulyoutube.core.model.PlaybackPosition
import dev.satoshi.usefulyoutube.core.model.VideoId
import dev.satoshi.usefulyoutube.core.testing.data.TestData
import dev.satoshi.usefulyoutube.core.testing.repository.FakeHistoryRepository
import dev.satoshi.usefulyoutube.core.testing.repository.FakePlaylistRepository
import dev.satoshi.usefulyoutube.core.testing.repository.FakeSearchRepository
import dev.satoshi.usefulyoutube.core.testing.repository.FakeVideoRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class HistoryUseCasesTest {
    private val repository = FakeHistoryRepository()

    @Test
    fun `watched videos are recorded, removed and cleared`() =
        runTest {
            ObserveWatchHistoryUseCase(repository)().test {
                assertThat(awaitItem()).isEmpty()

                RecordWatchUseCase(repository)(TestData.video("a"))
                RecordWatchUseCase(repository)(TestData.video("b"))
                awaitItem()
                assertThat(awaitItem().map { it.video.id.value }).containsExactly("b", "a")

                RemoveFromHistoryUseCase(repository)(VideoId("b"))
                assertThat(awaitItem().map { it.video.id.value }).containsExactly("a")

                ClearHistoryUseCase(repository)()
                assertThat(awaitItem()).isEmpty()
            }
        }

    @Test
    fun `player finds videos from the history`() =
        runTest {
            val watched = TestData.video("watched")
            RecordWatchUseCase(repository)(watched)
            val observeVideo =
                ObserveVideoUseCase(FakeVideoRepository(), FakeSearchRepository(), FakePlaylistRepository(), repository)

            assertThat(observeVideo(VideoId("watched")).first()).isEqualTo(watched)
        }

    @Test
    fun `saved playback position is observed and removed with the video`() =
        runTest {
            val position = PlaybackPosition(position = 2.minutes, duration = 10.minutes)
            RecordWatchUseCase(repository)(TestData.video("a"))

            SavePlaybackPositionUseCase(repository)(VideoId("a"), position)
            assertThat(ObservePlaybackPositionsUseCase(repository)().first()).containsOnly(VideoId("a") to position)

            RemoveFromHistoryUseCase(repository)(VideoId("a"))
            assertThat(ObservePlaybackPositionsUseCase(repository)().first()).isEmpty()
        }

    @Test
    fun `playback resumes where it stopped unless watched briefly or to the end`() =
        runTest {
            val getResumePosition = GetResumePositionUseCase(repository)
            val save = SavePlaybackPositionUseCase(repository)
            RecordWatchUseCase(repository)(TestData.video("a"))

            assertThat(getResumePosition(VideoId("a"))).isEqualTo(Duration.ZERO)
            save(VideoId("a"), PlaybackPosition(position = 2.minutes, duration = 10.minutes))
            assertThat(getResumePosition(VideoId("a"))).isEqualTo(2.minutes)
            save(VideoId("a"), PlaybackPosition(position = 4.seconds, duration = 10.minutes))
            assertThat(getResumePosition(VideoId("a"))).isEqualTo(Duration.ZERO)
            save(VideoId("a"), PlaybackPosition(position = 10.minutes - 9.seconds, duration = 10.minutes))
            assertThat(getResumePosition(VideoId("a"))).isEqualTo(Duration.ZERO)
            save(VideoId("a"), PlaybackPosition(position = 10.minutes - 10.seconds, duration = 10.minutes))
            assertThat(getResumePosition(VideoId("a"))).isEqualTo(10.minutes - 10.seconds)
        }

    @Test
    fun `progress is the watched part of the video`() {
        assertThat(PlaybackPosition(position = 2.minutes, duration = 8.minutes).progress).isEqualTo(0.25f)
        assertThat(PlaybackPosition(position = 9.minutes, duration = 8.minutes).progress).isEqualTo(1f)
        assertThat(PlaybackPosition(position = 2.minutes, duration = Duration.ZERO).progress).isEqualTo(0f)
    }

    @Test
    fun `video is finished when less than the end margin is left`() {
        assertThat(PlaybackPosition(position = 10.minutes - 9.seconds, duration = 10.minutes).isFinished).isTrue()
        assertThat(PlaybackPosition(position = 10.minutes, duration = 10.minutes).isFinished).isTrue()
        assertThat(PlaybackPosition(position = 10.minutes - 10.seconds, duration = 10.minutes).isFinished).isFalse()
        assertThat(PlaybackPosition(position = 2.minutes, duration = Duration.ZERO).isFinished).isFalse()
    }
}
