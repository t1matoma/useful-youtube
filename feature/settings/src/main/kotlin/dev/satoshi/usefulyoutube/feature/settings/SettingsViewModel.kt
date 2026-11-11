package dev.satoshi.usefulyoutube.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.satoshi.usefulyoutube.core.domain.usecase.ObserveSettingsUseCase
import dev.satoshi.usefulyoutube.core.domain.usecase.SetRecommendationsEnabledUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        observeSettings: ObserveSettingsUseCase,
        private val setRecommendationsEnabled: SetRecommendationsEnabledUseCase,
    ) : ViewModel() {
        val uiState: StateFlow<SettingsUiState> =
            observeSettings()
                .map { SettingsUiState(isLoading = false, recommendationsEnabled = it.recommendationsEnabled) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), SettingsUiState())

        fun onEvent(event: SettingsUiEvent) {
            when (event) {
                is SettingsUiEvent.RecommendationsToggled -> {
                    viewModelScope.launch { setRecommendationsEnabled(event.enabled) }
                }
            }
        }

        private companion object {
            const val STOP_TIMEOUT_MILLIS = 5_000L
        }
    }
