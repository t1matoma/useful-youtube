package dev.satoshi.usefulyoutube.feature.playlists

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import dev.satoshi.usefulyoutube.core.domain.repository.AccountPlaylist
import dev.satoshi.usefulyoutube.core.domain.shorts.DurationShortsDetector
import dev.satoshi.usefulyoutube.core.domain.usecase.ClassifyVideoUseCase
import dev.satoshi.usefulyoutube.core.domain.usecase.CreatePlaylistUseCase
import dev.satoshi.usefulyoutube.core.domain.usecase.ObservePlaylistsUseCase
import dev.satoshi.usefulyoutube.core.domain.usecase.SyncAccountPlaylistsUseCase
import dev.satoshi.usefulyoutube.core.model.AccountError
import dev.satoshi.usefulyoutube.core.model.AppResult
import dev.satoshi.usefulyoutube.core.model.AuthError
import dev.satoshi.usefulyoutube.core.model.DataError
import dev.satoshi.usefulyoutube.core.model.PlaylistSource
import dev.satoshi.usefulyoutube.core.testing.MainDispatcherRule
import dev.satoshi.usefulyoutube.core.testing.collectInBackground
import dev.satoshi.usefulyoutube.core.testing.repository.FakeAccountAuthorizer
import dev.satoshi.usefulyoutube.core.testing.repository.FakeMirroredPlaylistRepository
import dev.satoshi.usefulyoutube.core.testing.repository.FakePlaylistRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class PlaylistsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakePlaylistRepository()
    private val mirror = FakeMirroredPlaylistRepository()
    private val authorizer = FakeAccountAuthorizer()
    private var remotePlaylists: AppResult<List<AccountPlaylist>, DataError> = AppResult.Success(emptyList())
    private var consentScreen: AppResult<IntentSender?, AuthError> = AppResult.Success(null)

    // Created after MainDispatcherRule has replaced the main dispatcher, which viewModelScope captures.
    private val viewModel by lazy {
        PlaylistsViewModel(
            observePlaylists = ObservePlaylistsUseCase(repository),
            createPlaylist = CreatePlaylistUseCase(repository),
            syncAccountPlaylists =
                SyncAccountPlaylistsUseCase(
                    authorizer = authorizer,
                    playlistsReader = { remotePlaylists },
                    mirroredPlaylists = mirror,
                    classifyVideo = ClassifyVideoUseCase(DurationShortsDetector()),
                ),
            accountConsent = { consentScreen },
        )
    }

    private fun intentSender(): IntentSender =
        PendingIntent
            .getActivity(RuntimeEnvironment.getApplication(), 0, Intent(), PendingIntent.FLAG_IMMUTABLE)
            .intentSender

    @Test
    fun `app and youtube playlists are shown apart and synced on open`() =
        runTest {
            repository.givenPlaylist("Later")
            repository.givenPlaylist("From YouTube", PlaylistSource.YOUTUBE)
            remotePlaylists = AppResult.Success(listOf(AccountPlaylist("PLa", "From YouTube", emptyList())))
            collectInBackground(viewModel.uiState)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state.localPlaylists.map { it.title }).containsExactly("Later")
            assertThat(state.youtubePlaylists.map { it.title }).containsExactly("From YouTube")
            assertThat(state.account).isEqualTo(AccountState.CONNECTED)
            assertThat(state.isSyncing).isFalse()
            assertThat(mirror.mirroredPlaylists?.map { it.remoteId }).isEqualTo(listOf("PLa"))
        }

    @Test
    fun `account without consent is not connected and can be connected`() =
        runTest {
            authorizer.error = AuthError.ConsentRequired
            consentScreen = AppResult.Success(intentSender())
            collectInBackground(viewModel.uiState)
            advanceUntilIdle()
            assertThat(viewModel.uiState.value.account).isEqualTo(AccountState.NOT_CONNECTED)
            assertThat(viewModel.uiState.value.syncError).isNull()

            viewModel.onEvent(PlaylistsUiEvent.ConnectAccountClicked)
            advanceUntilIdle()
            assertThat(viewModel.uiState.value.consentScreen).isNotNull()

            viewModel.onEvent(PlaylistsUiEvent.ConsentLaunched)
            authorizer.error = null
            viewModel.onEvent(PlaylistsUiEvent.ConsentFinished(granted = true))
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.consentScreen).isNull()
            assertThat(viewModel.uiState.value.account).isEqualTo(AccountState.CONNECTED)
        }

    @Test
    fun `declined consent is reported`() =
        runTest {
            authorizer.error = AuthError.ConsentRequired
            consentScreen = AppResult.Success(intentSender())
            collectInBackground(viewModel.uiState)
            viewModel.onEvent(PlaylistsUiEvent.ConnectAccountClicked)
            advanceUntilIdle()
            viewModel.onEvent(PlaylistsUiEvent.ConsentLaunched)

            viewModel.onEvent(PlaylistsUiEvent.ConsentFinished(granted = false))
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.syncError).isEqualTo(AccountError.Auth(AuthError.Cancelled))
            assertThat(viewModel.uiState.value.account).isEqualTo(AccountState.NOT_CONNECTED)
        }

    @Test
    fun `network error keeps the stored playlists and can be refreshed`() =
        runTest {
            repository.givenPlaylist("Stored", PlaylistSource.YOUTUBE)
            remotePlaylists = AppResult.Failure(DataError.NoNetwork)
            collectInBackground(viewModel.uiState)
            advanceUntilIdle()
            assertThat(viewModel.uiState.value.syncError).isEqualTo(AccountError.Remote(DataError.NoNetwork))
            assertThat(
                viewModel.uiState.value.youtubePlaylists
                    .map { it.title },
            ).containsExactly("Stored")

            remotePlaylists = AppResult.Success(emptyList())
            viewModel.onEvent(PlaylistsUiEvent.Refresh)
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.syncError).isNull()
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
