package dev.satoshi.usefulyoutube.core.domain.repository

import dev.satoshi.usefulyoutube.core.model.AppSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    /** The defaults of [AppSettings] until the user changes something. */
    fun observeSettings(): Flow<AppSettings>

    suspend fun setRecommendationsEnabled(enabled: Boolean)
}
