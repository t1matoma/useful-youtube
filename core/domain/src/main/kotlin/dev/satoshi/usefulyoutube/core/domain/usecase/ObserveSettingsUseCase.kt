package dev.satoshi.usefulyoutube.core.domain.usecase

import dev.satoshi.usefulyoutube.core.domain.repository.SettingsRepository
import dev.satoshi.usefulyoutube.core.model.AppSettings
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveSettingsUseCase
    @Inject
    constructor(
        private val settingsRepository: SettingsRepository,
    ) {
        operator fun invoke(): Flow<AppSettings> = settingsRepository.observeSettings()
    }
