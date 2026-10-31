package dev.satoshi.usefulyoutube.core.testing.repository

import dev.satoshi.usefulyoutube.core.domain.channel.ChannelReference
import dev.satoshi.usefulyoutube.core.domain.repository.ChannelRepository
import dev.satoshi.usefulyoutube.core.model.AppResult
import dev.satoshi.usefulyoutube.core.model.Channel
import dev.satoshi.usefulyoutube.core.model.ChannelError
import dev.satoshi.usefulyoutube.core.model.ChannelId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeChannelRepository(
    initialChannels: List<Channel> = emptyList(),
) : ChannelRepository {
    private val channels = MutableStateFlow(initialChannels)

    /** Channels that [addChannel] can resolve, keyed by reference. */
    val remoteChannels = mutableMapOf<ChannelReference, Channel>()

    /** When set, [addChannel] fails with this error. */
    var addChannelError: ChannelError? = null

    override fun observeChannels(): Flow<List<Channel>> = channels

    override suspend fun getChannels(): List<Channel> = channels.value

    override suspend fun addChannel(reference: ChannelReference): AppResult<Channel, ChannelError> {
        val channel = remoteChannels[reference]
        val error =
            when {
                addChannelError != null -> addChannelError
                channel == null -> ChannelError.NotFound
                channels.value.any { it.id == channel.id } -> ChannelError.AlreadySubscribed
                else -> null
            }
        if (error != null || channel == null) return AppResult.Failure(error ?: ChannelError.NotFound)
        channels.update { it + channel }
        return AppResult.Success(channel)
    }

    override suspend fun removeChannel(id: ChannelId) {
        channels.update { list -> list.filterNot { it.id == id } }
    }

    override suspend fun importChannels(channels: List<Channel>): Int {
        val existingIds =
            this.channels.value
                .map { it.id }
                .toSet()
        val newChannels = channels.filterNot { it.id in existingIds }.distinctBy { it.id }
        this.channels.update { it + newChannels }
        return newChannels.size
    }
}
