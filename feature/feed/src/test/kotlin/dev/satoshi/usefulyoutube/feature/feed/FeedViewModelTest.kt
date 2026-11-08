package dev.satoshi.usefulyoutube.feature.feed

import assertk.assertThat
import assertk.assertions.isEqualTo
import dev.satoshi.usefulyoutube.core.domain.repository.VideoRepository
import dev.satoshi.usefulyoutube.core.domain.shorts.DurationShortsDetector
import dev.satoshi.usefulyoutube.core.domain.usecase.ClassifyVideoUseCase
import dev.satoshi.usefulyoutube.core.domain.usecase.ObserveFeedUseCase
import dev.satoshi.usefulyoutube.core.domain.usecase.ObservePlaybackPositionsUseCase
import dev.satoshi.usefulyoutube.core.domain.usecase.RefreshFeedUseCase
import dev.satoshi.usefulyoutube.core.model.DataError
import dev.satoshi.usefulyoutube.core.model.PlaybackPosition
import dev.satoshi.usefulyoutube.core.model.VideoContentKind
import dev.satoshi.usefulyoutube.core.testing.MainDispatcherRule
import dev.satoshi.usefulyoutube.core.testing.collectInBackground
import dev.satoshi.usefulyoutube.core.testing.data.TestData
import dev.satoshi.usefulyoutube.core.testing.repository.FakeChannelRepository
import dev.satoshi.usefulyoutube.core.testing.repository.FakeHistoryRepository
import dev.satoshi.usefulyoutube.core.testing.repository.FakeVideoRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalCoroutinesApi::class)
class FeedViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())

    private val channelRepository = FakeChannelRepository(listOf(TestData.firstChannel))
    private val videoRepository = FakeVideoRepository()

    private fun TestScope.createViewModel(
        repository: VideoRepository = videoRepository,
        historyRepository: FakeHistoryRepository = FakeHistoryRepository(),
    ): FeedViewModel {
        val viewModel =
            FeedViewModel(
                observeFeed = ObserveFeedUseCase(repository),
                observePlaybackPositions = ObservePlaybackPositionsUseCase(historyRepository),
                refreshFeed =
                    RefreshFeedUseCase(channelRepository, repository, ClassifyVideoUseCase(DurationShortsDetector())),
            )
        collectInBackground(viewModel.uiState)
        return viewModel
    }

    @Test
    fun `starts loading and refreshes on start`() =
        runTest {
            val newVideo =
                TestData.video(
                    id = "new",
                    duration = 12.minutes,
                    contentKind = VideoContentKind.UNCLASSIFIED,
                )
            videoRepository.remoteFeeds[TestData.firstChannel.id] = listOf(newVideo)

            val viewModel = createViewModel()
            assertThat(viewModel.uiState.value).isEqualTo(FeedUiState.Loading)

            advanceUntilIdle()
            assertThat(viewModel.uiState.value).isEqualTo(
                FeedUiState.Content(
                    videos = listOf(newVideo.copy(contentKind = VideoContentKind.REGULAR)),
                    isRefreshing = false,
                    refreshError = null,
                ),
            )
        }

    @Test
    fun `empty feed after refresh is empty state`() =
        runTest {
            val viewModel = createViewModel()
            advanceUntilIdle()

            assertThat(viewModel.uiState.value).isEqualTo(FeedUiState.Empty(isRefreshing = false))
        }

    @Test
    fun `failed refresh without stored videos is error state and can be retried`() =
        runTest {
            videoRepository.failingChannels[TestData.firstChannel.id] = DataError.NoNetwork
            val viewModel = createViewModel()
            advanceUntilIdle()
            assertThat(viewModel.uiState.value).isEqualTo(FeedUiState.Error(DataError.NoNetwork, isRefreshing = false))

            videoRepository.failingChannels.clear()
            viewModel.onEvent(FeedUiEvent.Refresh)
            advanceUntilIdle()

            assertThat(viewModel.uiState.value).isEqualTo(FeedUiState.Empty(isRefreshing = false))
        }

    @Test
    fun `failed refresh over stored videos keeps them and reports error once`() =
        runTest {
            val stored = TestData.video(id = "stored")
            val repository = FakeVideoRepository(listOf(stored)).apply { detailsError = DataError.QuotaExceeded }
            val viewModel = createViewModel(repository)
            advanceUntilIdle()
            assertThat(viewModel.uiState.value).isEqualTo(
                FeedUiState.Content(listOf(stored), isRefreshing = false, refreshError = DataError.QuotaExceeded),
            )

            viewModel.onEvent(FeedUiEvent.RefreshErrorShown)
            advanceUntilIdle()

            assertThat(viewModel.uiState.value).isEqualTo(
                FeedUiState.Content(listOf(stored), isRefreshing = false, refreshError = null),
            )
        }

    @Test
    fun `content shows where watched videos stopped`() =
        runTest {
            val watched = TestData.video(id = "watched")
            val watchedPosition = PlaybackPosition(position = 1.minutes, duration = 10.minutes)
            val viewModel =
                createViewModel(
                    FakeVideoRepository(listOf(watched)),
                    FakeHistoryRepository(initialPositions = mapOf(watched.id to watchedPosition)),
                )
            advanceUntilIdle()

            val content = viewModel.uiState.value as FeedUiState.Content
            assertThat(content.playbackPositions).isEqualTo(mapOf(watched.id to watchedPosition))
        }
}
