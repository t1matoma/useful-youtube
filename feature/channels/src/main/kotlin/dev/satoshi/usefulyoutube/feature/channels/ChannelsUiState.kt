package dev.satoshi.usefulyoutube.feature.channels

import dev.satoshi.usefulyoutube.core.model.Channel
import dev.satoshi.usefulyoutube.core.model.ChannelError

data class ChannelsUiState(
    val isLoading: Boolean = true,
    val channels: List<Channel> = emptyList(),
    val input: String = "",
    val isAdding: Boolean = false,
    val addError: ChannelError? = null,
    /** Channel waiting for the user to confirm its removal. */
    val channelPendingRemoval: Channel? = null,
)
