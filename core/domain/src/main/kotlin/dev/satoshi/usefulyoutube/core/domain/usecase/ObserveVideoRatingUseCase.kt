package dev.satoshi.usefulyoutube.core.domain.usecase

import dev.satoshi.usefulyoutube.core.domain.repository.VideoRatingRepository
import dev.satoshi.usefulyoutube.core.model.VideoId
import dev.satoshi.usefulyoutube.core.model.VideoRating
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveVideoRatingUseCase
    @Inject
    constructor(
        private val ratingRepository: VideoRatingRepository,
    ) {
        operator fun invoke(id: VideoId): Flow<VideoRating> = ratingRepository.observeRating(id)
    }
