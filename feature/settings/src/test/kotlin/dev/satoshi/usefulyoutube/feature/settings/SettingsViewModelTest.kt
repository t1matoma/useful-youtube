package dev.satoshi.usefulyoutube.feature.settings

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import dev.satoshi.usefulyoutube.core.domain.usecase.ObserveSettingsUseCase
import dev.satoshi.usefulyoutube.core.domain.usecase.SetRecommendationsEnabledUseCase
import dev.satoshi.usefulyoutube.core.model.AppSettings
import dev.satoshi.usefulyoutube.core.testing.MainDispatcherRule
import dev.satoshi.usefulyoutube.core.testing.collectInBackground
import dev.satoshi.usefulyoutube.core.testing.repository.FakeSettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeSettingsRepository()

    // Created after MainDispatcherRule has replaced the main dispatcher, which viewModelScope captures.
    private val viewModel by lazy {
        SettingsViewModel(ObserveSettingsUseCase(repository), SetRecommendationsEnabledUseCase(repository))
    }

    @Test
    fun `recommendations are enabled by default`() =
        runTest {
            assertThat(viewModel.uiState.value).isEqualTo(SettingsUiState(isLoading = true))

            collectInBackground(viewModel.uiState)
            advanceUntilIdle()

            assertThat(viewModel.uiState.value)
                .isEqualTo(SettingsUiState(isLoading = false, recommendationsEnabled = true))
        }

    @Test
    fun `turning recommendations off is saved and shown`() =
        runTest {
            collectInBackground(viewModel.uiState)

            viewModel.onEvent(SettingsUiEvent.RecommendationsToggled(enabled = false))
            advanceUntilIdle()

            assertThat(repository.currentSettings).isEqualTo(AppSettings(recommendationsEnabled = false))
            assertThat(viewModel.uiState.value.recommendationsEnabled).isFalse()
        }
}
