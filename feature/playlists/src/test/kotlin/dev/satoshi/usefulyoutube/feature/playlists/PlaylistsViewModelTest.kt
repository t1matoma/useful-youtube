package dev.satoshi.usefulyoutube.feature.playlists

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import dev.satoshi.usefulyoutube.core.domain.usecase.CreatePlaylistUseCase
import dev.satoshi.usefulyoutube.core.domain.usecase.ObservePlaylistsUseCase
import dev.satoshi.usefulyoutube.core.model.PlaylistSource
import dev.satoshi.usefulyoutube.core.testing.MainDispatcherRule
import dev.satoshi.usefulyoutube.core.testing.collectInBackground
import dev.satoshi.usefulyoutube.core.testing.repository.FakePlaylistRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlaylistsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakePlaylistRepository()

    // Created after MainDispatcherRule has replaced the main dispatcher, which viewModelScope captures.
    private val viewModel by lazy {
        PlaylistsViewModel(ObservePlaylistsUseCase(repository), CreatePlaylistUseCase(repository))
    }

    @Test
    fun `app playlists are shown`() =
        runTest {
            repository.givenPlaylist("Later")
            repository.givenPlaylist("From YouTube", PlaylistSource.YOUTUBE)
            collectInBackground(viewModel.uiState)
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.isLoading).isFalse()
            assertThat(
                viewModel.uiState.value.localPlaylists
                    .map { it.title },
            ).containsExactly("Later")
        }

    @Test
    fun `playlist is created from the dialog`() =
        runTest {
            collectInBackground(viewModel.uiState)

            viewModel.onEvent(PlaylistsUiEvent.CreateClicked)
            viewModel.onEvent(PlaylistsUiEvent.NewTitleChanged("Kotlin talks"))
            advanceUntilIdle()
            assertThat(viewModel.uiState.value.newPlaylistTitle).isEqualTo("Kotlin talks")

            viewModel.onEvent(PlaylistsUiEvent.CreateConfirmed)
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.newPlaylistTitle).isNull()
            assertThat(
                viewModel.uiState.value.localPlaylists
                    .map { it.title },
            ).containsExactly("Kotlin talks")
        }

    @Test
    fun `dismissed or blank dialog creates nothing`() =
        runTest {
            collectInBackground(viewModel.uiState)

            viewModel.onEvent(PlaylistsUiEvent.CreateClicked)
            viewModel.onEvent(PlaylistsUiEvent.CreateConfirmed)
            viewModel.onEvent(PlaylistsUiEvent.NewTitleChanged("Unsaved"))
            viewModel.onEvent(PlaylistsUiEvent.CreateDismissed)
            viewModel.onEvent(PlaylistsUiEvent.NewTitleChanged("Ignored"))
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.newPlaylistTitle).isNull()
            assertThat(viewModel.uiState.value.localPlaylists).isEmpty()
        }
}
