package dev.satoshi.usefulyoutube.core.domain.usecase

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import dev.satoshi.usefulyoutube.core.model.VideoId
import dev.satoshi.usefulyoutube.core.testing.data.TestData
import dev.satoshi.usefulyoutube.core.testing.repository.FakeHistoryRepository
import dev.satoshi.usefulyoutube.core.testing.repository.FakePlaylistRepository
import dev.satoshi.usefulyoutube.core.testing.repository.FakeSearchRepository
import dev.satoshi.usefulyoutube.core.testing.repository.FakeVideoRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

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
}
