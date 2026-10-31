package dev.satoshi.usefulyoutube.feature.feed

sealed interface FeedUiEvent {
    data object Refresh : FeedUiEvent

    data object RefreshErrorShown : FeedUiEvent
}
