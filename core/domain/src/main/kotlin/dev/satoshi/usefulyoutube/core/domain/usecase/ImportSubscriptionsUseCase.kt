package dev.satoshi.usefulyoutube.core.domain.usecase

import dev.satoshi.usefulyoutube.core.domain.repository.ChannelRepository
import dev.satoshi.usefulyoutube.core.domain.repository.SubscriptionsFileReader
import dev.satoshi.usefulyoutube.core.model.AppResult
import dev.satoshi.usefulyoutube.core.model.ImportError
import javax.inject.Inject

/** Imports subscriptions from a Google Takeout file; returns the number of newly added channels. */
class ImportSubscriptionsUseCase
    @Inject
    constructor(
        private val fileReader: SubscriptionsFileReader,
        private val channelRepository: ChannelRepository,
    ) {
        suspend operator fun invoke(fileUri: String): AppResult<Int, ImportError> =
            when (val channels = fileReader.read(fileUri)) {
                is AppResult.Failure -> {
                    channels
                }

                is AppResult.Success -> {
                    if (channels.value.isEmpty()) {
                        AppResult.Failure(ImportError.NoChannels)
                    } else {
                        AppResult.Success(channelRepository.importChannels(channels.value))
                    }
                }
            }
    }
