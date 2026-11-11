package dev.satoshi.usefulyoutube.core.domain.usecase

import dev.satoshi.usefulyoutube.core.domain.repository.AccountAuthorizer
import dev.satoshi.usefulyoutube.core.domain.repository.VideoRatingRepository
import dev.satoshi.usefulyoutube.core.domain.repository.withAccessToken
import dev.satoshi.usefulyoutube.core.model.AccountError
import dev.satoshi.usefulyoutube.core.model.AppResult
import dev.satoshi.usefulyoutube.core.model.Video
import dev.satoshi.usefulyoutube.core.model.VideoRating
import javax.inject.Inject

/**
 * Likes or dislikes the video in the YouTube account, or removes the rating with [VideoRating.NONE]
 * (`videos.rate`, 50 quota units). The rating is stored only after YouTube accepted it.
 */
class RateVideoUseCase
    @Inject
    constructor(
        private val authorizer: AccountAuthorizer,
        private val ratingRepository: VideoRatingRepository,
    ) {
        suspend operator fun invoke(
            video: Video,
            rating: VideoRating,
        ): AppResult<Unit, AccountError> {
            val result = authorizer.withAccessToken { ratingRepository.sendRating(it, video.id, rating) }
            if (result is AppResult.Success) ratingRepository.saveRating(video, rating)
            return result
        }
    }
