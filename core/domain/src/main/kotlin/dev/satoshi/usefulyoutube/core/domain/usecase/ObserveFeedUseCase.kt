package dev.satoshi.usefulyoutube.core.domain.usecase

import dev.satoshi.usefulyoutube.core.domain.repository.VideoRepository
import dev.satoshi.usefulyoutube.core.model.Video
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveFeedUseCase
    @Inject
    constructor(
        private val videoRepository: VideoRepository,
    ) {
        operator fun invoke(): Flow<List<Video>> = videoRepository.observeFeed()
    }
