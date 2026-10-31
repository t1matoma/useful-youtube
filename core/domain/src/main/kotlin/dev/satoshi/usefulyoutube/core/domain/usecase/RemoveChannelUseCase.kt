package dev.satoshi.usefulyoutube.core.domain.usecase

import dev.satoshi.usefulyoutube.core.domain.repository.ChannelRepository
import dev.satoshi.usefulyoutube.core.model.ChannelId
import javax.inject.Inject

class RemoveChannelUseCase
    @Inject
    constructor(
        private val channelRepository: ChannelRepository,
    ) {
        suspend operator fun invoke(id: ChannelId) = channelRepository.removeChannel(id)
    }
