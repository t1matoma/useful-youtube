package dev.satoshi.usefulyoutube.core.domain.repository

import dev.satoshi.usefulyoutube.core.model.AppResult
import dev.satoshi.usefulyoutube.core.model.DataError
import dev.satoshi.usefulyoutube.core.model.RatedVideo
import dev.satoshi.usefulyoutube.core.model.Video
import dev.satoshi.usefulyoutube.core.model.VideoId
import dev.satoshi.usefulyoutube.core.model.VideoRating
import kotlinx.coroutines.flow.Flow

/**
 * Likes and dislikes. The YouTube account is the source of truth; the stored copy shows the rating offline and
 * feeds the recommendations.
 */
interface VideoRatingRepository {
    /** The stored rating; [VideoRating.NONE] for a video that is not rated or not checked yet. */
    fun observeRating(id: VideoId): Flow<VideoRating>

    fun observeRatedVideos(): Flow<List<RatedVideo>>

    /** Stores the rating of the [video]; [VideoRating.NONE] forgets it. */
    suspend fun saveRating(
        video: Video,
        rating: VideoRating,
    )

    /** Reads the rating from the account with `videos.getRating`. */
    suspend fun fetchRating(
        accessToken: String,
        id: VideoId,
    ): AppResult<VideoRating, DataError>

    /** Rates the video in the account with `videos.rate`. */
    suspend fun sendRating(
        accessToken: String,
        id: VideoId,
        rating: VideoRating,
    ): AppResult<Unit, DataError>
}
