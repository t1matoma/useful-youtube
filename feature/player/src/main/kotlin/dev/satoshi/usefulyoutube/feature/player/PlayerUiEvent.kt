package dev.satoshi.usefulyoutube.feature.player

sealed interface PlayerUiEvent {
    data object DescriptionToggled : PlayerUiEvent

    data object SubscribeToChannel : PlayerUiEvent
}
