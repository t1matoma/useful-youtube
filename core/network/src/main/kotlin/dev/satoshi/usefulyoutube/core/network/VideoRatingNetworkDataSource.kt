package dev.satoshi.usefulyoutube.core.network

import dev.satoshi.usefulyoutube.core.model.AppResult
import dev.satoshi.usefulyoutube.core.model.DataError

/** Likes and dislikes of the account that granted the access token, through the Data API. */
interface VideoRatingNetworkDataSource {
    /** The rating as the API names it: [RATING_LIKE], [RATING_DISLIKE] or [RATING_NONE]. */
    suspend fun getVideoRating(
        accessToken: String,
        videoId: String,
    ): AppResult<String, DataError>

    suspend fun rateVideo(
        accessToken: String,
        videoId: String,
        rating: String,
    ): AppResult<Unit, DataError>

    companion object {
        const val RATING_LIKE = "like"
        const val RATING_DISLIKE = "dislike"
        const val RATING_NONE = "none"
    }
}
