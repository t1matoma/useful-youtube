package dev.satoshi.usefulyoutube.core.domain.usecase

import dev.satoshi.usefulyoutube.core.domain.repository.AccountAuthorizer
import dev.satoshi.usefulyoutube.core.domain.repository.AccountSubscriptionsReader
import dev.satoshi.usefulyoutube.core.domain.repository.ChannelRepository
import dev.satoshi.usefulyoutube.core.model.AppResult
import dev.satoshi.usefulyoutube.core.model.Channel
import dev.satoshi.usefulyoutube.core.model.DataError
import dev.satoshi.usefulyoutube.core.model.ImportError
import javax.inject.Inject

/**
 * Imports the subscriptions of the user's YouTube account; returns the number of newly added channels.
 * Channels that are already subscribed in the app are kept as they are.
 */
class ImportAccountSubscriptionsUseCase
    @Inject
    constructor(
        private val authorizer: AccountAuthorizer,
        private val subscriptionsReader: AccountSubscriptionsReader,
        private val channelRepository: ChannelRepository,
    ) {
        suspend operator fun invoke(): AppResult<Int, ImportError> {
            val channels =
                when (val result = readSubscriptions(retryUnauthorized = true)) {
                    is AppResult.Failure -> return result
                    is AppResult.Success -> result.value
                }
            return if (channels.isEmpty()) {
                AppResult.Failure(ImportError.NoChannels)
            } else {
                AppResult.Success(channelRepository.importChannels(channels))
            }
        }

        /** A cached token can expire or be revoked, so a rejected token is replaced once. */
        private suspend fun readSubscriptions(retryUnauthorized: Boolean): AppResult<List<Channel>, ImportError> {
            val token =
                when (val result = authorizer.getAccessToken()) {
                    is AppResult.Failure -> return AppResult.Failure(ImportError.Auth(result.error))
                    is AppResult.Success -> result.value
                }
            return when (val result = subscriptionsReader.read(token)) {
                is AppResult.Success -> {
                    result
                }

                is AppResult.Failure -> {
                    if (result.error == DataError.Unauthorized && retryUnauthorized) {
                        authorizer.clearAccessToken(token)
                        readSubscriptions(retryUnauthorized = false)
                    } else {
                        AppResult.Failure(ImportError.Remote(result.error))
                    }
                }
            }
        }
    }
