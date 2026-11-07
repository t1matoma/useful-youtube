package dev.satoshi.usefulyoutube.core.data.subscriptions

import dev.satoshi.usefulyoutube.core.data.di.IoDispatcher
import dev.satoshi.usefulyoutube.core.data.mapper.toChannel
import dev.satoshi.usefulyoutube.core.domain.repository.AccountSubscriptionsReader
import dev.satoshi.usefulyoutube.core.model.AppResult
import dev.satoshi.usefulyoutube.core.model.Channel
import dev.satoshi.usefulyoutube.core.model.DataError
import dev.satoshi.usefulyoutube.core.model.map
import dev.satoshi.usefulyoutube.core.network.YouTubeNetworkDataSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal class NetworkAccountSubscriptionsReader
    @Inject
    constructor(
        private val networkDataSource: YouTubeNetworkDataSource,
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : AccountSubscriptionsReader {
        override suspend fun read(accessToken: String): AppResult<List<Channel>, DataError> =
            withContext(ioDispatcher) {
                networkDataSource
                    .getMySubscriptions(accessToken)
                    .map { subscriptions -> subscriptions.map { it.toChannel() }.distinctBy { it.id } }
            }
    }
