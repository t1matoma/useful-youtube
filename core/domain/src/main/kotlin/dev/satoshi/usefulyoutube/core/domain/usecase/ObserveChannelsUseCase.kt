package dev.satoshi.usefulyoutube.core.domain.usecase

import dev.satoshi.usefulyoutube.core.domain.repository.ChannelRepository
import dev.satoshi.usefulyoutube.core.model.Channel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveChannelsUseCase
    @Inject
    constructor(
        private val channelRepository: ChannelRepository,
    ) {
        operator fun invoke(): Flow<List<Channel>> = channelRepository.observeChannels()
    }
