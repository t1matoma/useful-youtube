package dev.satoshi.usefulyoutube.feature.search

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import dev.satoshi.usefulyoutube.core.domain.repository.SearchPage
import dev.satoshi.usefulyoutube.core.domain.repository.SearchSession
import dev.satoshi.usefulyoutube.core.domain.shorts.DurationShortsDetector
import dev.satoshi.usefulyoutube.core.domain.usecase.ClassifyVideoUseCase
import dev.satoshi.usefulyoutube.core.domain.usecase.DeleteRecentSearchQueryUseCase
import dev.satoshi.usefulyoutube.core.domain.usecase.LoadMoreSearchResultsUseCase
import dev.satoshi.usefulyoutube.core.domain.usecase.ObservePlaybackPositionsUseCase
import dev.satoshi.usefulyoutube.core.domain.usecase.ObserveRecentSearchQueriesUseCase
import dev.satoshi.usefulyoutube.core.domain.usecase.ObserveSearchResultsUseCase
import dev.satoshi.usefulyoutube.core.domain.usecase.SearchVideosUseCase
import dev.satoshi.usefulyoutube.core.model.DataError
import dev.satoshi.usefulyoutube.core.model.SearchOrder
import dev.satoshi.usefulyoutube.core.model.VideoContentKind
import dev.satoshi.usefulyoutube.core.testing.MainDispatcherRule
import dev.satoshi.usefulyoutube.core.testing.collectInBackground
import dev.satoshi.usefulyoutube.core.testing.data.TestData
import dev.satoshi.usefulyoutube.core.testing.repository.FakeHistoryRepository
import dev.satoshi.usefulyoutube.core.testing.repository.FakeSearchRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeSearchRepository()

    private fun createViewModel(): SearchViewModel {
        val classifyVideo = ClassifyVideoUseCase(DurationShortsDetector())
        return SearchViewModel(
            observeSearchResults = ObserveSearchResultsUseCase(repository),
            observeRecentQueries = ObserveRecentSearchQueriesUseCase(repository),
            observePlaybackPositions = ObservePlaybackPositionsUseCase(FakeHistoryRepository()),
            searchVideos = SearchVideosUseCase(repository, classifyVideo),
            loadMoreResults = LoadMoreSearchResultsUseCase(repository, classifyVideo),
            deleteRecentQuery = DeleteRecentSearchQueryUseCase(repository),
        )
    }

    private fun found(id: String) =
        TestData.video(id = id, duration = 10.minutes, contentKind = VideoContentKind.UNCLASSIFIED)

    @Test
    fun `submitted query shows found videos`() =
        runTest {
            repository.remotePages["kotlin" to null] = SearchPage(listOf(found("a")), nextPageToken = "page2")
            val viewModel = createViewModel()
            collectInBackground(viewModel.uiState)

            viewModel.onEvent(SearchUiEvent.QueryChanged("kotlin"))
            viewModel.onEvent(SearchUiEvent.SearchSubmitted)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state.isSearching).isFalse()
            assertThat(state.results?.videos?.map { it.id.value }).isEqualTo(listOf("a"))
            assertThat(state.results?.canLoadMore).isEqualTo(true)
            assertThat(state.recentQueries).containsExactly("kotlin")
        }

    @Test
    fun `progress is shown while searching`() =
        runTest {
            repository.fetchGate = CompletableDeferred()
            val viewModel = createViewModel()
            collectInBackground(viewModel.uiState)

            viewModel.onEvent(SearchUiEvent.RecentQuerySelected("kotlin"))
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.isSearching).isTrue()
            assertThat(viewModel.uiState.value.query).isEqualTo("kotlin")
            repository.fetchGate?.complete(Unit)
            advanceUntilIdle()
            assertThat(viewModel.uiState.value.isSearching).isFalse()
        }

    @Test
    fun `stored search is shown when the screen opens`() =
        runTest {
            repository.saveFirstPage(SearchSession("compose", SearchOrder.DATE, null), listOf(TestData.video("v")))
            val viewModel = createViewModel()
            collectInBackground(viewModel.uiState)
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.query).isEqualTo("compose")
            assertThat(viewModel.uiState.value.order).isEqualTo(SearchOrder.DATE)
        }

    @Test
    fun `blank query is not searched`() =
        runTest {
            val viewModel = createViewModel()
            collectInBackground(viewModel.uiState)

            viewModel.onEvent(SearchUiEvent.QueryChanged("  "))
            viewModel.onEvent(SearchUiEvent.SearchSubmitted)
            advanceUntilIdle()

            assertThat(repository.fetchedPages).isEmpty()
        }

    @Test
    fun `another order searches the current query again`() =
        runTest {
            val viewModel = createViewModel()
            collectInBackground(viewModel.uiState)
            viewModel.onEvent(SearchUiEvent.QueryChanged("kotlin"))
            viewModel.onEvent(SearchUiEvent.SearchSubmitted)
            advanceUntilIdle()

            viewModel.onEvent(SearchUiEvent.OrderSelected(SearchOrder.DATE))
            viewModel.onEvent(SearchUiEvent.OrderSelected(SearchOrder.DATE))
            advanceUntilIdle()

            assertThat(repository.fetchedPages).containsExactly(
                Triple("kotlin", SearchOrder.RELEVANCE, null),
                Triple("kotlin", SearchOrder.DATE, null),
            )
            assertThat(viewModel.uiState.value.order).isEqualTo(SearchOrder.DATE)
        }

    @Test
    fun `search error is shown and cleared by the next search`() =
        runTest {
            repository.fetchError = DataError.QuotaExceeded
            val viewModel = createViewModel()
            collectInBackground(viewModel.uiState)
            viewModel.onEvent(SearchUiEvent.QueryChanged("kotlin"))
            viewModel.onEvent(SearchUiEvent.SearchSubmitted)
            advanceUntilIdle()
            assertThat(viewModel.uiState.value.searchError).isEqualTo(DataError.QuotaExceeded)

            repository.fetchError = null
            viewModel.onEvent(SearchUiEvent.SearchSubmitted)
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.searchError).isNull()
        }

    @Test
    fun `next page is loaded on request and its error is kept separately`() =
        runTest {
            repository.saveFirstPage(
                SearchSession("kotlin", SearchOrder.RELEVANCE, "page2"),
                listOf(TestData.video("a")),
            )
            repository.remotePages["kotlin" to "page2"] = SearchPage(listOf(found("b")), nextPageToken = null)
            repository.fetchError = DataError.NoNetwork
            val viewModel = createViewModel()
            collectInBackground(viewModel.uiState)

            viewModel.onEvent(SearchUiEvent.LoadMoreRequested)
            advanceUntilIdle()
            assertThat(viewModel.uiState.value.loadMoreError).isEqualTo(DataError.NoNetwork)
            assertThat(viewModel.uiState.value.searchError).isNull()

            repository.fetchError = null
            viewModel.onEvent(SearchUiEvent.LoadMoreRequested)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state.loadMoreError).isNull()
            assertThat(state.results?.videos?.map { it.id.value }).isEqualTo(listOf("a", "b"))
            assertThat(state.results?.canLoadMore).isEqualTo(false)
        }

    @Test
    fun `recent query can be deleted`() =
        runTest {
            repository.saveRecentQuery("kotlin")
            val viewModel = createViewModel()
            collectInBackground(viewModel.uiState)

            viewModel.onEvent(SearchUiEvent.RecentQueryDeleted("kotlin"))
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.recentQueries).isEmpty()
        }
}
