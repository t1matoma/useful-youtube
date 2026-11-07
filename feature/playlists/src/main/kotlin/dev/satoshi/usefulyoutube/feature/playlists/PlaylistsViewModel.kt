package dev.satoshi.usefulyoutube.feature.playlists

import android.content.IntentSender
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.satoshi.usefulyoutube.core.auth.AccountConsent
import dev.satoshi.usefulyoutube.core.domain.usecase.CreatePlaylistUseCase
import dev.satoshi.usefulyoutube.core.domain.usecase.ObservePlaylistsUseCase
import dev.satoshi.usefulyoutube.core.domain.usecase.SyncAccountPlaylistsUseCase
import dev.satoshi.usefulyoutube.core.model.AccountError
import dev.satoshi.usefulyoutube.core.model.AppResult
import dev.satoshi.usefulyoutube.core.model.AuthError
import dev.satoshi.usefulyoutube.core.model.PlaylistSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The YouTube playlists are loaded once when the tab opens and on pull-to-refresh. */
@HiltViewModel
class PlaylistsViewModel
    @Inject
    constructor(
        observePlaylists: ObservePlaylistsUseCase,
        private val createPlaylist: CreatePlaylistUseCase,
        private val syncAccountPlaylists: SyncAccountPlaylistsUseCase,
        private val accountConsent: AccountConsent,
    ) : ViewModel() {
        private data class ScreenState(
            val account: AccountState = AccountState.CHECKING,
            val isSyncing: Boolean = false,
            val syncError: AccountError? = null,
            val consentScreen: IntentSender? = null,
            val isAwaitingConsent: Boolean = false,
            val newPlaylistTitle: String? = null,
        )

        private val screenState = MutableStateFlow(ScreenState())

        val uiState: StateFlow<PlaylistsUiState> =
            combine(observePlaylists(), screenState) { playlists, screen ->
                PlaylistsUiState(
                    isLoading = false,
                    localPlaylists = playlists.filter { it.source == PlaylistSource.LOCAL },
                    youtubePlaylists = playlists.filter { it.source == PlaylistSource.YOUTUBE },
                    account = screen.account,
                    isSyncing = screen.isSyncing,
                    syncError = screen.syncError,
                    consentScreen = screen.consentScreen,
                    newPlaylistTitle = screen.newPlaylistTitle,
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), PlaylistsUiState())

        init {
            sync()
        }

        fun onEvent(event: PlaylistsUiEvent) {
            when (event) {
                PlaylistsUiEvent.CreateClicked -> {
                    screenState.update { it.copy(newPlaylistTitle = "") }
                }

                is PlaylistsUiEvent.NewTitleChanged -> {
                    screenState.update { state ->
                        state.copy(newPlaylistTitle = state.newPlaylistTitle?.let { event.title })
                    }
                }

                PlaylistsUiEvent.CreateConfirmed -> {
                    create()
                }

                PlaylistsUiEvent.CreateDismissed -> {
                    screenState.update { it.copy(newPlaylistTitle = null) }
                }

                PlaylistsUiEvent.Refresh -> {
                    sync()
                }

                PlaylistsUiEvent.ConnectAccountClicked -> {
                    connect()
                }

                PlaylistsUiEvent.ConsentLaunched -> {
                    screenState.update { it.copy(consentScreen = null, isAwaitingConsent = it.consentScreen != null) }
                }

                is PlaylistsUiEvent.ConsentFinished -> {
                    onConsentFinished(event.granted)
                }
            }
        }

        private fun create() {
            val title = screenState.value.newPlaylistTitle?.takeIf { it.isNotBlank() } ?: return
            screenState.update { it.copy(newPlaylistTitle = null) }
            viewModelScope.launch { createPlaylist(title) }
        }

        private fun sync() {
            if (screenState.value.isSyncing) return
            screenState.update { it.copy(isSyncing = true, syncError = null) }
            viewModelScope.launch {
                val result = syncAccountPlaylists()
                screenState.update { state ->
                    when (result) {
                        is AppResult.Success -> state.copy(isSyncing = false, account = AccountState.CONNECTED)
                        is AppResult.Failure -> state.withSyncFailure(result.error)
                    }
                }
            }
        }

        /** Without consent the account is simply not connected; that is not an error. */
        private fun ScreenState.withSyncFailure(error: AccountError): ScreenState =
            when {
                error == AccountError.Auth(AuthError.ConsentRequired) -> {
                    copy(isSyncing = false, account = AccountState.NOT_CONNECTED)
                }

                error is AccountError.Remote -> {
                    copy(isSyncing = false, account = AccountState.CONNECTED, syncError = error)
                }

                else -> {
                    copy(
                        isSyncing = false,
                        account = if (account == AccountState.CHECKING) AccountState.NOT_CONNECTED else account,
                        syncError = error,
                    )
                }
            }

        private fun connect() {
            val screen = screenState.value
            if (screen.isSyncing || screen.isAwaitingConsent || screen.consentScreen != null) return
            viewModelScope.launch {
                when (val consent = accountConsent.consentScreen()) {
                    is AppResult.Failure -> {
                        screenState.update { it.copy(syncError = AccountError.Auth(consent.error)) }
                    }

                    is AppResult.Success -> {
                        val sender = consent.value
                        if (sender ==
                            null
                        ) {
                            sync()
                        } else {
                            screenState.update { it.copy(consentScreen = sender, syncError = null) }
                        }
                    }
                }
            }
        }

        private fun onConsentFinished(granted: Boolean) {
            if (!screenState.value.isAwaitingConsent) return
            screenState.update { it.copy(isAwaitingConsent = false) }
            if (granted) {
                sync()
            } else {
                screenState.update { it.copy(syncError = AccountError.Auth(AuthError.Cancelled)) }
            }
        }

        private companion object {
            const val STOP_TIMEOUT_MILLIS = 5_000L
        }
    }
