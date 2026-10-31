package dev.satoshi.usefulyoutube.core.data.repository

import dev.satoshi.usefulyoutube.core.data.di.IoDispatcher
import dev.satoshi.usefulyoutube.core.data.mapper.toChannel
import dev.satoshi.usefulyoutube.core.data.mapper.toEntity
import dev.satoshi.usefulyoutube.core.database.dao.ChannelDao
import dev.satoshi.usefulyoutube.core.domain.channel.ChannelReference
import dev.satoshi.usefulyoutube.core.domain.repository.ChannelRepository
import dev.satoshi.usefulyoutube.core.model.AppResult
import dev.satoshi.usefulyoutube.core.model.Channel
import dev.satoshi.usefulyoutube.core.model.ChannelError
import dev.satoshi.usefulyoutube.core.model.ChannelId
import dev.satoshi.usefulyoutube.core.model.DataError
import dev.satoshi.usefulyoutube.core.network.YouTubeNetworkDataSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal class OfflineFirstChannelRepository
    @Inject
    constructor(
        private val channelDao: ChannelDao,
        private val networkDataSource: YouTubeNetworkDataSource,
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : ChannelRepository {
        override fun observeChannels(): Flow<List<Channel>> =
            channelDao.observeChannels().map { channels -> channels.map { it.toChannel() } }

        override suspend fun getChannels(): List<Channel> = channelDao.getChannels().map { it.toChannel() }

        override suspend fun addChannel(reference: ChannelReference): AppResult<Channel, ChannelError> {
            val channel =
                when (val resolved = withContext(ioDispatcher) { resolve(reference) }) {
                    is AppResult.Success -> resolved.value
                    is AppResult.Failure -> return resolved
                }
            val isNew = channelDao.insertOrIgnore(listOf(channel.toEntity())).single() != ALREADY_EXISTS
            return if (isNew) AppResult.Success(channel) else AppResult.Failure(ChannelError.AlreadySubscribed)
        }

        override suspend fun removeChannel(id: ChannelId) = channelDao.deleteChannel(id.value)

        override suspend fun importChannels(channels: List<Channel>): Int =
            channelDao.insertOrIgnore(channels.map { it.toEntity() }).count { it != ALREADY_EXISTS }

        private suspend fun resolve(reference: ChannelReference): AppResult<Channel, ChannelError> =
            when (reference) {
                is ChannelReference.ById -> {
                    when (val feed = networkDataSource.getChannelFeed(reference.id.value)) {
                        is AppResult.Success -> AppResult.Success(Channel(reference.id, feed.value.title))
                        is AppResult.Failure -> AppResult.Failure(feed.error.toChannelError())
                    }
                }

                is ChannelReference.ByHandle -> {
                    when (val found = networkDataSource.getChannelByHandle(reference.handle)) {
                        is AppResult.Success -> {
                            found.value
                                ?.let { AppResult.Success(Channel(ChannelId(it.id), it.snippet.title)) }
                                ?: AppResult.Failure(ChannelError.NotFound)
                        }

                        is AppResult.Failure -> {
                            AppResult.Failure(found.error.toChannelError())
                        }
                    }
                }
            }

        private fun DataError.toChannelError(): ChannelError =
            if (this == DataError.NotFound) ChannelError.NotFound else ChannelError.Remote(this)

        private companion object {
            const val ALREADY_EXISTS = -1L
        }
    }
