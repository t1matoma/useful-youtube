package dev.satoshi.usefulyoutube.feature.history

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import dev.satoshi.usefulyoutube.core.domain.usecase.ClearHistoryUseCase
import dev.satoshi.usefulyoutube.core.domain.usecase.ObservePlaybackPositionsUseCase
import dev.satoshi.usefulyoutube.core.domain.usecase.ObserveWatchHistoryUseCase
import dev.satoshi.usefulyoutube.core.domain.usecase.RemoveFromHistoryUseCase
import dev.satoshi.usefulyoutube.core.model.PlaybackPosition
import dev.satoshi.usefulyoutube.core.model.WatchedVideo
import dev.satoshi.usefulyoutube.core.testing.MainDispatcherRule
import dev.satoshi.usefulyoutube.core.testing.collectInBackground
import dev.satoshi.usefulyoutube.core.testing.data.TestData
import dev.satoshi.usefulyoutube.core.testing.repository.FakeHistoryRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.time.ZoneOffset
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository =
        FakeHistoryRepository(
            listOf(
                WatchedVideo(TestData.video("b"), Instant.parse("2026-11-07T09:00:00Z")),
                WatchedVideo(TestData.video("a"), Instant.parse("2026-11-06T09:00:00Z")),
            ),
            initialPositions = mapOf(TestData.video("a").id to PlaybackPosition(2.minutes, 10.minutes)),
        )

    // Created after MainDispatcherRule has replaced the main dispatcher, which viewModelScope captures.
    private val viewModel by lazy {
        HistoryViewModel(
            observeWatchHistory = ObserveWatchHistoryUseCase(repository),
            observePlaybackPositions = ObservePlaybackPositionsUseCase(repository),
            removeFromHistory = RemoveFromHistoryUseCase(repository),
            clearHistory = ClearHistoryUseCase(repository),
            clock =
                object : Clock {
                    override fun now() = Instant.parse("2026-11-07T12:00:00Z")
                },
            zone = ZoneOffset.UTC,
        )
    }

    @Test
    fun `history is shown by day`() =
        runTest {
            collectInBackground(viewModel.uiState)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state.isLoading).isFalse()
            assertThat(state.days.map { it.day }).containsExactly(HistoryDayLabel.Today, HistoryDayLabel.Yesterday)
            assertThat(state.playbackPositions[TestData.video("a").id]?.progress).isEqualTo(0.2f)
        }

    @Test
    fun `single video is removed`() =
        runTest {
            collectInBackground(viewModel.uiState)

            viewModel.onEvent(HistoryUiEvent.VideoRemoved(TestData.video("b").id))
            advanceUntilIdle()

            assertThat(
                viewModel.uiState.value.days
                    .flatMap { it.videos }
                    .map { it.id.value },
            ).containsExactly("a")
        }

    @Test
    fun `history is cleared only after confirmation`() =
        runTest {
            collectInBackground(viewModel.uiState)

            viewModel.onEvent(HistoryUiEvent.ClearClicked)
            advanceUntilIdle()
            assertThat(viewModel.uiState.value.isClearDialogVisible).isTrue()
            viewModel.onEvent(HistoryUiEvent.ClearDismissed)
            advanceUntilIdle()
            assertThat(viewModel.uiState.value.days.size).isEqualTo(2)

            viewModel.onEvent(HistoryUiEvent.ClearClicked)
            viewModel.onEvent(HistoryUiEvent.ClearConfirmed)
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.isClearDialogVisible).isFalse()
            assertThat(viewModel.uiState.value.days).isEmpty()
        }
}
