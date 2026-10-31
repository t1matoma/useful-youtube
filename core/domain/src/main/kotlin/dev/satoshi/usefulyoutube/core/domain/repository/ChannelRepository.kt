package dev.satoshi.usefulyoutube.core.domain.repository

import dev.satoshi.usefulyoutube.core.domain.channel.ChannelReference
import dev.satoshi.usefulyoutube.core.model.AppResult
import dev.satoshi.usefulyoutube.core.model.Channel
import dev.satoshi.usefulyoutube.core.model.ChannelError
import dev.satoshi.usefulyoutube.core.model.ChannelId
import kotlinx.coroutines.flow.Flow

interface ChannelRepository {
    fun observeChannels(): Flow<List<Channel>>

    suspend fun getChannels(): List<Channel>

    /** Resolves the [reference] remotely and subscribes to the channel. */
    suspend fun addChannel(reference: ChannelReference): AppResult<Channel, ChannelError>

    suspend fun removeChannel(id: ChannelId)

    /** Saves the [channels], skipping already subscribed ones; returns the number of new subscriptions. */
    suspend fun importChannels(channels: List<Channel>): Int
}
