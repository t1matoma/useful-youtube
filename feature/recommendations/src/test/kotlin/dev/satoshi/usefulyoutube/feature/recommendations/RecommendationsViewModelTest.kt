package dev.satoshi.usefulyoutube.feature.recommendations

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNull
import dev.satoshi.usefulyoutube.core.domain.recommendations.RecommendationRanker
import dev.satoshi.usefulyoutube.core.domain.shorts.DurationShortsDetector
import dev.satoshi.usefulyoutube.core.domain.usecase.ClassifyVideoUseCase
import dev.satoshi.usefulyoutube.core.domain.usecase.ObserveRecommendationsUseCase
import dev.satoshi.usefulyoutube.core.domain.usecase.RefreshRecommendationsUseCase
import dev.satoshi.usefulyoutube.core.model.Channel
import dev.satoshi.usefulyoutube.core.model.ChannelId
import dev.satoshi.usefulyoutube.core.model.DataError
import dev.satoshi.usefulyoutube.core.model.Video
import dev.satoshi.usefulyoutube.core.model.VideoContentKind
import dev.satoshi.usefulyoutube.core.model.WatchedVideo
import dev.satoshi.usefulyoutube.core.testing.MainDispatcherRule
import dev.satoshi.usefulyoutube.core.testing.collectInBackground
import dev.satoshi.usefulyoutube.core.testing.data.TestData
import dev.satoshi.usefulyoutube.core.testing.repository.FakeChannelRepository
import dev.satoshi.usefulyoutube.core.testing.repository.FakeHistoryRepository
import dev.satoshi.usefulyoutube.core.testing.repository.FakeRecommendationRepository
import dev.satoshi.usefulyoutube.core.testing.repository.FakeVideoRatingRepository
import dev.satoshi.usefulyoutube.core.testing.repository.FakeVideoRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.time.Clock
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class RecommendationsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val now = Instant.parse("2026-11-10T12:00:00Z")
    private val favorite = TestData.firstChannel
    private val featured = Channel(ChannelId("UCffffffffffffffffffffff"), "Featured channel")
    private val watched = TestData.video("watched", favorite, publishedAt = now)
    private val recommendations = FakeRecommendationRepository()
    private var feed = emptyList<Video>()
    private var history = FakeHistoryRepository()

    // Created after MainDispatcherRule has replaced the main dispatcher, which viewModelScope captures.
    private val viewModel by lazy {
        val channels = FakeChannelRepository(listOf(favorite))
        val ratings = FakeVideoRatingRepository()
        RecommendationsViewModel(
            observeRecommendations =
                ObserveRecommendationsUseCase(
                    FakeVideoRepository(feed),
                    recommendations,
                    history,
                    ratings,
                    RecommendationRanker(),
                    object : Clock {
                        override fun now() = now
                    },
                ),
            refreshRecommendations =
                RefreshRecommendationsUseCase(
                    recommendations,
                    channels,
                    history,
                    ratings,
                    ClassifyVideoUseCase(DurationShortsDetector()),
                ),
        )
    }

    private fun givenWatchedFavorite() {
        history = FakeHistoryRepository(initialHistory = listOf(WatchedVideo(watched, now)))
        recommendations.featuredChannels[favorite.id] = listOf(featured.id)
        recommendations.remoteVideos[featured.id] =
            listOf(TestData.video("found", featured, publishedAt = now, contentKind = VideoContentKind.UNCLASSIFIED))
    }

    @Test
    fun `nothing to recommend is an empty state`() =
        runTest {
            collectInBackground(viewModel.uiState)
            advanceUntilIdle()

            assertThat(viewModel.uiState.value).isEqualTo(RecommendationsUiState.Empty(isRefreshing = false))
        }

    @Test
    fun `opening the tab looks for videos and shows them with the unwatched feed`() =
        runTest {
            givenWatchedFavorite()
            feed = listOf(watched, TestData.video("own", favorite, publishedAt = now))
            collectInBackground(viewModel.uiState)
            advanceUntilIdle()

            val state = viewModel.uiState.value as RecommendationsUiState.Content
            assertThat(state.videos.map { it.id.value }).containsExactly("own", "found")
            assertThat(state.isRefreshing).isEqualTo(false)
            assertThat(state.refreshError).isNull()
        }

    @Test
    fun `a failed refresh without stored videos is an error and retry recovers`() =
        runTest {
            givenWatchedFavorite()
            recommendations.videosError = DataError.NoNetwork
            collectInBackground(viewModel.uiState)
            advanceUntilIdle()

            assertThat(viewModel.uiState.value)
                .isEqualTo(RecommendationsUiState.Error(DataError.NoNetwork, isRefreshing = false))

            recommendations.videosError = null
            viewModel.onEvent(RecommendationsUiEvent.Refresh)
            advanceUntilIdle()

            assertThat(viewModel.uiState.value).isInstanceOf<RecommendationsUiState.Content>()
        }

    @Test
    fun `a failed refresh over stored videos is shown once`() =
        runTest {
            feed = listOf(TestData.video("own", favorite, publishedAt = now))
            givenWatchedFavorite()
            recommendations.featuredError = DataError.QuotaExceeded
            collectInBackground(viewModel.uiState)
            advanceUntilIdle()

            val failed = viewModel.uiState.value as RecommendationsUiState.Content
            assertThat(failed.videos.map { it.id.value }).containsExactly("own")
            assertThat(failed.refreshError).isEqualTo(DataError.QuotaExceeded)

            viewModel.onEvent(RecommendationsUiEvent.RefreshErrorShown)
            advanceUntilIdle()

            assertThat((viewModel.uiState.value as RecommendationsUiState.Content).refreshError).isNull()
        }
}
