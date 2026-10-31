package dev.satoshi.usefulyoutube.core.domain.channel

import dev.satoshi.usefulyoutube.core.model.ChannelId

/** A channel the user referred to, before it is resolved remotely. */
sealed interface ChannelReference {
    data class ById(
        val id: ChannelId,
    ) : ChannelReference

    data class ByHandle(
        val handle: String,
    ) : ChannelReference
}
