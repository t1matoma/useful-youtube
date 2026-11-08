package dev.satoshi.usefulyoutube.feature.playlists

import androidx.lifecycle.SavedStateHandle
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import dev.satoshi.usefulyoutube.core.domain.usecase.DeletePlaylistUseCase
import dev.satoshi.usefulyoutube.core.domain.usecase.ObservePlaybackPositionsUseCase
import dev.satoshi.usefulyoutube.core.domain.usecase.ObservePlaylistUseCase
import dev.satoshi.usefulyoutube.core.domain.usecase.RenamePlaylistUseCase
import dev.satoshi.usefulyoutube.core.domain.usecase.SetVideoInPlaylistUseCase
import dev.satoshi.usefulyoutube.core.model.PlaylistId
import dev.satoshi.usefulyoutube.core.model.PlaylistSource
import dev.satoshi.usefulyoutube.core.testing.MainDispatcherRule
import dev.satoshi.usefulyoutube.core.testing.collectInBackground
import dev.satoshi.usefulyoutube.core.testing.data.TestData
import dev.satoshi.usefulyoutube.core.testing.repository.FakeHistoryRepository
import dev.satoshi.usefulyoutube.core.testing.repository.FakePlaylistRepository
import dev.satoshi.usefulyoutube.feature.playlists.navigation.PlaylistDestination
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlaylistViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakePlaylistRepository()

    private fun createViewModel(id: PlaylistId) =
        PlaylistViewModel(
            savedStateHandle = SavedStateHandle(mapOf(PlaylistDestination.PLAYLIST_ID_ARGUMENT to id.value)),
            observePlaylist = ObservePlaylistUseCase(repository),
            observePlaybackPositions = ObservePlaybackPositionsUseCase(FakeHistoryRepository()),
            renamePlaylist = RenamePlaylistUseCase(repository),
            deletePlaylist = DeletePlaylistUseCase(repository),
            setVideoInPlaylist = SetVideoInPlaylistUseCase(repository),
        )

    private val PlaylistViewModel.content get() = uiState.value as PlaylistUiState.Content

    @Test
    fun `playlist videos are shown and can be removed`() =
        runTest {
            val id = repository.givenPlaylist("Later", videos = listOf(TestData.video("a"), TestData.video("b")))
            val viewModel = createViewModel(id)
            collectInBackground(viewModel.uiState)

            viewModel.onEvent(PlaylistUiEvent.VideoRemoved(TestData.video("a").id))
            advanceUntilIdle()

            assertThat(
                viewModel.content.playlist.videos
                    .map { it.id.value },
            ).containsExactly("b")
        }

    @Test
    fun `playlist is renamed from the dialog`() =
        runTest {
            val id = repository.givenPlaylist("Old")
            val viewModel = createViewModel(id)
            collectInBackground(viewModel.uiState)
            advanceUntilIdle()

            viewModel.onEvent(PlaylistUiEvent.RenameClicked)
            advanceUntilIdle()
            assertThat(viewModel.content.dialog).isEqualTo(PlaylistDialog.Rename("Old"))
            viewModel.onEvent(PlaylistUiEvent.TitleChanged("New"))
            viewModel.onEvent(PlaylistUiEvent.RenameConfirmed)
            advanceUntilIdle()

            assertThat(viewModel.content.dialog).isNull()
            assertThat(viewModel.content.playlist.playlist.title).isEqualTo("New")
        }

    @Test
    fun `deleted playlist is not found`() =
        runTest {
            val id = repository.givenPlaylist("Doomed")
            val viewModel = createViewModel(id)
            collectInBackground(viewModel.uiState)
            advanceUntilIdle()

            viewModel.onEvent(PlaylistUiEvent.DeleteClicked)
            advanceUntilIdle()
            assertThat(viewModel.content.dialog).isEqualTo(PlaylistDialog.ConfirmDelete)
            viewModel.onEvent(PlaylistUiEvent.DeleteConfirmed)
            advanceUntilIdle()

            assertThat(viewModel.uiState.value).isEqualTo(PlaylistUiState.NotFound)
        }

    @Test
    fun `youtube playlist ignores changes`() =
        runTest {
            val id = repository.givenPlaylist("Mix", PlaylistSource.YOUTUBE, listOf(TestData.video("a")))
            val viewModel = createViewModel(id)
            collectInBackground(viewModel.uiState)
            advanceUntilIdle()

            viewModel.onEvent(PlaylistUiEvent.RenameClicked)
            viewModel.onEvent(PlaylistUiEvent.VideoRemoved(TestData.video("a").id))
            advanceUntilIdle()

            assertThat(viewModel.content.dialog).isNull()
            assertThat(viewModel.content.playlist.videos.size).isEqualTo(1)
        }
}
