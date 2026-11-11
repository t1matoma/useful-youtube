package dev.satoshi.usefulyoutube.core.data.repository

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.satoshi.usefulyoutube.core.data.di.IoDispatcher
import dev.satoshi.usefulyoutube.core.domain.repository.SettingsRepository
import dev.satoshi.usefulyoutube.core.model.AppSettings
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** The settings are a few flags, so they live in the app's preferences file instead of the database. */
internal class PreferencesSettingsRepository
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : SettingsRepository {
        /** Opening the file reads it from the disk, so it happens on first use, off the main thread. */
        private val preferences by lazy { context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE) }

        override fun observeSettings(): Flow<AppSettings> =
            callbackFlow {
                val listener =
                    SharedPreferences.OnSharedPreferenceChangeListener {
                        changed,
                        _,
                        ->
                        trySend(changed.read())
                    }
                preferences.registerOnSharedPreferenceChangeListener(listener)
                send(preferences.read())
                awaitClose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
            }.conflate().distinctUntilChanged().flowOn(ioDispatcher)

        override suspend fun setRecommendationsEnabled(enabled: Boolean) =
            withContext(ioDispatcher) {
                preferences.edit().putBoolean(RECOMMENDATIONS_ENABLED, enabled).apply()
            }

        private fun SharedPreferences.read(): AppSettings {
            val defaults = AppSettings()
            return AppSettings(
                recommendationsEnabled = getBoolean(RECOMMENDATIONS_ENABLED, defaults.recommendationsEnabled),
            )
        }

        private companion object {
            const val FILE_NAME = "settings"
            const val RECOMMENDATIONS_ENABLED = "recommendations_enabled"
        }
    }
