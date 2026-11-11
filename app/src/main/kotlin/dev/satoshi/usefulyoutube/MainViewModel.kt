package dev.satoshi.usefulyoutube

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.satoshi.usefulyoutube.core.domain.usecase.ObserveSettingsUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** The settings that shape the app outside of a single screen. */
@HiltViewModel
class MainViewModel
    @Inject
    constructor(
        observeSettings: ObserveSettingsUseCase,
    ) : ViewModel() {
        val uiState: StateFlow<MainUiState> =
            observeSettings()
                .map { MainUiState(recommendationsEnabled = it.recommendationsEnabled) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), MainUiState())

        private companion object {
            const val STOP_TIMEOUT_MILLIS = 5_000L
        }
    }
