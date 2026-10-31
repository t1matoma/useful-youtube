package dev.satoshi.usefulyoutube.core.domain.usecase

import dev.satoshi.usefulyoutube.core.domain.channel.ChannelInputParser
import dev.satoshi.usefulyoutube.core.domain.channel.ChannelReference
import dev.satoshi.usefulyoutube.core.domain.repository.ChannelRepository
import dev.satoshi.usefulyoutube.core.model.AppResult
import dev.satoshi.usefulyoutube.core.model.Channel
import dev.satoshi.usefulyoutube.core.model.ChannelError
import javax.inject.Inject

/** Subscribes to a channel given as a link, a handle or an id. */
class AddChannelUseCase
    @Inject
    constructor(
        private val inputParser: ChannelInputParser,
        private val channelRepository: ChannelRepository,
    ) {
        suspend operator fun invoke(input: String): AppResult<Channel, ChannelError> {
            val reference =
                when (val parsed = inputParser.parse(input)) {
                    is AppResult.Success -> parsed.value
                    is AppResult.Failure -> return parsed
                }
            if (reference is ChannelReference.ById && isSubscribed(reference)) {
                return AppResult.Failure(ChannelError.AlreadySubscribed)
            }
            return channelRepository.addChannel(reference)
        }

        private suspend fun isSubscribed(reference: ChannelReference.ById): Boolean =
            channelRepository.getChannels().any { it.id == reference.id }
    }
