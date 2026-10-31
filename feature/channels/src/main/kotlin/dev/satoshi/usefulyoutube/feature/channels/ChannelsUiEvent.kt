package dev.satoshi.usefulyoutube.feature.channels

import dev.satoshi.usefulyoutube.core.model.Channel

sealed interface ChannelsUiEvent {
    data class InputChanged(
        val input: String,
    ) : ChannelsUiEvent

    data object AddClicked : ChannelsUiEvent

    data class RemoveRequested(
        val channel: Channel,
    ) : ChannelsUiEvent

    data object RemoveConfirmed : ChannelsUiEvent

    data object RemoveDismissed : ChannelsUiEvent
}
