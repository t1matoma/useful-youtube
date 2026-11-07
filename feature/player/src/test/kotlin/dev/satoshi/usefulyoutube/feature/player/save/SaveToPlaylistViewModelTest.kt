package dev.satoshi.usefulyoutube.feature.player.save

import androidx.lifecycle.SavedStateHandle
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import assertk.assertions.isTrue
import dev.satoshi.usefulyoutube.core.domain.usecase.CreatePlaylistUseCase
import dev.satoshi.usefulyoutube.core.domain.usecase.ObservePlaylistsContainingVideoUseCase
import dev.satoshi.usefulyoutube.core.domain.usecase.ObservePlaylistsUseCase
import dev.satoshi.usefulyoutube.core.domain.usecase.ObserveVideoUseCase
import dev.satoshi.usefulyoutube.core.domain.usecase.SetVideoInPlaylistUseCase
import dev.satoshi.usefulyoutube.core.model.PlaylistSource
import dev.satoshi.usefulyoutube.core.testing.MainDispatcherRule
import dev.satoshi.usefulyoutube.core.testing.collectInBackground
import dev.satoshi.usefulyoutube.core.testing.data.TestData
import dev.satoshi.usefulyoutube.core.testing.repository.FakeHistoryRepository
import dev.satoshi.usefulyoutube.core.testing.repository.FakePlaylistRepository
import dev.satoshi.usefulyoutube.core.testing.repository.FakeSearchRepository
import dev.satoshi.usefulyoutube.core.testing.repository.FakeVideoRepository
import dev.satoshi.usefulyoutube.feature.player.navigation.PlayerDestination
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SaveToPlaylistViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val video = TestData.video("abc")
    private val playlists = FakePlaylistRepository()
    private val viewModel by lazy {
        SaveToPlaylistViewModel(
            savedStateHandle = SavedStateHandle(mapOf(PlayerDestination.VIDEO_ID_ARGUMENT to video.id.value)),
            observePlaylists = ObservePlaylistsUseCase(playlists),
            observePlaylistsContainingVideo = ObservePlaylistsContainingVideoUseCase(playlists),
            observeVideo =
                ObserveVideoUseCase(
                    FakeVideoRepository(listOf(video)),
                    FakeSearchRepository(),
                    playlists,
                    FakeHistoryRepository(),
                ),
            setVideoInPlaylist = SetVideoInPlaylistUseCase(playlists),
            createPlaylist = CreatePlaylistUseCase(playlists),
        )
    }

    @Test
    fun `sheet offers app playlists and ticks the ones with the video`() =
        runTest {
            val later = playlists.givenPlaylist("Later", videos = listOf(video))
            val music = playlists.givenPlaylist("Music")
            playlists.givenPlaylist("From YouTube", PlaylistSource.YOUTUBE)
            collectInBackground(viewModel.uiState)

            viewModel.onEvent(SaveToPlaylistUiEvent.SaveClicked)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state.isSheetVisible).isTrue()
            assertThat(state.playlists).containsExactly(
                PlaylistOption(later, "Later", containsVideo = true),
                PlaylistOption(music, "Music", containsVideo = false),
            )
        }

    @Test
    fun `toggling a playlist adds and removes the video`() =
        runTest {
            val later = playlists.givenPlaylist("Later")
            collectInBackground(viewModel.uiState)

            viewModel.onEvent(SaveToPlaylistUiEvent.PlaylistToggled(later, containsVideo = true))
            advanceUntilIdle()
            assertThat(
                viewModel.uiState.value.playlists
                    .single()
                    .containsVideo,
            ).isTrue()

            viewModel.onEvent(SaveToPlaylistUiEvent.PlaylistToggled(later, containsVideo = false))
            advanceUntilIdle()
            assertThat(
                viewModel.uiState.value.playlists
                    .single()
                    .containsVideo,
            ).isEqualTo(false)
        }

    @Test
    fun `new playlist is created with the video`() =
        runTest {
            collectInBackground(viewModel.uiState)

            viewModel.onEvent(SaveToPlaylistUiEvent.NewPlaylistClicked)
            viewModel.onEvent(SaveToPlaylistUiEvent.NewPlaylistTitleChanged("Talks"))
            viewModel.onEvent(SaveToPlaylistUiEvent.NewPlaylistConfirmed)
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.newPlaylistTitle).isNull()
            assertThat(
                playlists.currentPlaylists
                    .single()
                    .playlist.title,
            ).isEqualTo("Talks")
            assertThat(playlists.currentPlaylists.single().videos).containsExactly(video)
        }

    @Test
    fun `sheet follows the video that is playing now`() =
        runTest {
            val other = TestData.video("other")
            val later = playlists.givenPlaylist("Later", videos = listOf(other))
            collectInBackground(viewModel.uiState)
            advanceUntilIdle()
            assertThat(
                viewModel.uiState.value.playlists
                    .single()
                    .containsVideo,
            ).isEqualTo(false)

            viewModel.onEvent(SaveToPlaylistUiEvent.VideoChanged(other.id))
            advanceUntilIdle()

            assertThat(
                viewModel.uiState.value.playlists
                    .single(),
            ).isEqualTo(PlaylistOption(later, "Later", containsVideo = true))
        }
}
