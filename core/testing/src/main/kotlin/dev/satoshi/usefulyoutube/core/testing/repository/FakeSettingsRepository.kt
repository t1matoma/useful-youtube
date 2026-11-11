package dev.satoshi.usefulyoutube.core.testing.repository

import dev.satoshi.usefulyoutube.core.domain.repository.SettingsRepository
import dev.satoshi.usefulyoutube.core.model.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeSettingsRepository(
    initialSettings: AppSettings = AppSettings(),
) : SettingsRepository {
    private val settings = MutableStateFlow(initialSettings)

    val currentSettings: AppSettings
        get() = settings.value

    override fun observeSettings(): Flow<AppSettings> = settings

    override suspend fun setRecommendationsEnabled(enabled: Boolean) {
        settings.update { it.copy(recommendationsEnabled = enabled) }
    }
}
