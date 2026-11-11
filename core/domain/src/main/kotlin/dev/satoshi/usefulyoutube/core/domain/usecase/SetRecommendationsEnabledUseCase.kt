package dev.satoshi.usefulyoutube.core.domain.usecase

import dev.satoshi.usefulyoutube.core.domain.repository.SettingsRepository
import javax.inject.Inject

class SetRecommendationsEnabledUseCase
    @Inject
    constructor(
        private val settingsRepository: SettingsRepository,
    ) {
        suspend operator fun invoke(enabled: Boolean) = settingsRepository.setRecommendationsEnabled(enabled)
    }
