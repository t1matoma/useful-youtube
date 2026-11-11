package dev.satoshi.usefulyoutube.feature.settings

sealed interface SettingsUiEvent {
    data class RecommendationsToggled(
        val enabled: Boolean,
    ) : SettingsUiEvent
}
