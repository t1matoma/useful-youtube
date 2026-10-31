package dev.satoshi.usefulyoutube.core.domain.usecase

import dev.satoshi.usefulyoutube.core.domain.repository.VideoRepository
import dev.satoshi.usefulyoutube.core.model.Video
import dev.satoshi.usefulyoutube.core.model.VideoId
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveVideoUseCase
    @Inject
    constructor(
        private val videoRepository: VideoRepository,
    ) {
        operator fun invoke(id: VideoId): Flow<Video?> = videoRepository.observeVideo(id)
    }
