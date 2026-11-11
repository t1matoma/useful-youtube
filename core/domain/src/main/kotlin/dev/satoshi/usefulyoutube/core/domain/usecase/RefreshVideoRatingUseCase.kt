package dev.satoshi.usefulyoutube.core.domain.usecase

import dev.satoshi.usefulyoutube.core.domain.repository.AccountAuthorizer
import dev.satoshi.usefulyoutube.core.domain.repository.VideoRatingRepository
import dev.satoshi.usefulyoutube.core.domain.repository.withAccessToken
import dev.satoshi.usefulyoutube.core.model.AccountError
import dev.satoshi.usefulyoutube.core.model.AppResult
import dev.satoshi.usefulyoutube.core.model.Video
import javax.inject.Inject

/**
 * Updates the stored rating of the video from the YouTube account (1 quota unit), so a like given in another
 * app shows up. Without a connected account it fails with `AuthError.ConsentRequired` and changes nothing.
 */
class RefreshVideoRatingUseCase
    @Inject
    constructor(
        private val authorizer: AccountAuthorizer,
        private val ratingRepository: VideoRatingRepository,
    ) {
        suspend operator fun invoke(video: Video): AppResult<Unit, AccountError> =
            when (val result = authorizer.withAccessToken { ratingRepository.fetchRating(it, video.id) }) {
                is AppResult.Failure -> {
                    result
                }

                is AppResult.Success -> {
                    ratingRepository.saveRating(video, result.value)
                    AppResult.Success(Unit)
                }
            }
    }
