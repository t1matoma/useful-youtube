package dev.satoshi.usefulyoutube.core.domain.usecase

import dev.satoshi.usefulyoutube.core.domain.repository.AccountAuthorizer
import dev.satoshi.usefulyoutube.core.domain.repository.AccountSubscriptionsReader
import dev.satoshi.usefulyoutube.core.domain.repository.ChannelRepository
import dev.satoshi.usefulyoutube.core.domain.repository.withAccessToken
import dev.satoshi.usefulyoutube.core.model.AccountError
import dev.satoshi.usefulyoutube.core.model.AppResult
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
                when (val result = authorizer.withAccessToken { subscriptionsReader.read(it) }) {
                    is AppResult.Failure -> return AppResult.Failure(result.error.toImportError())
                    is AppResult.Success -> result.value
                }
            return if (channels.isEmpty()) {
                AppResult.Failure(ImportError.NoChannels)
            } else {
                AppResult.Success(channelRepository.importChannels(channels))
            }
        }

        private fun AccountError.toImportError(): ImportError =
            when (this) {
                is AccountError.Auth -> ImportError.Auth(cause)
                is AccountError.Remote -> ImportError.Remote(cause)
            }
    }
