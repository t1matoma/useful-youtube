package dev.satoshi.usefulyoutube.feature.player

sealed interface PlayerUiEvent {
    data object DescriptionToggled : PlayerUiEvent

    data object SubscribeToChannel : PlayerUiEvent

    data object PreviousClicked : PlayerUiEvent

    data object NextClicked : PlayerUiEvent

    /** The player finished the current video; a playlist goes on with the next one. */
    data object VideoEnded : PlayerUiEvent
}
