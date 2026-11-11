package dev.satoshi.usefulyoutube.feature.recommendations

sealed interface RecommendationsUiEvent {
    data object Refresh : RecommendationsUiEvent

    data object RefreshErrorShown : RecommendationsUiEvent
}
