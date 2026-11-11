package dev.satoshi.usefulyoutube.core.testing.repository

import dev.satoshi.usefulyoutube.core.domain.repository.VideoRatingRepository
import dev.satoshi.usefulyoutube.core.model.AppResult
import dev.satoshi.usefulyoutube.core.model.DataError
import dev.satoshi.usefulyoutube.core.model.RatedVideo
import dev.satoshi.usefulyoutube.core.model.Video
import dev.satoshi.usefulyoutube.core.model.VideoId
import dev.satoshi.usefulyoutube.core.model.VideoRating
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** [remoteRatings] plays the YouTube account; requests with a token other than [validToken] are unauthorized. */
class FakeVideoRatingRepository(
    initialRatedVideos: List<RatedVideo> = emptyList(),
    private val validToken: String = "token",
) : VideoRatingRepository {
    private val ratedVideos = MutableStateFlow(initialRatedVideos)

    val remoteRatings = mutableMapOf<VideoId, VideoRating>()

    /** When set, [fetchRating] and [sendRating] fail with this error. */
    var remoteError: DataError? = null

    val currentRatedVideos: List<RatedVideo>
        get() = ratedVideos.value

    override fun observeRating(id: VideoId): Flow<VideoRating> =
        ratedVideos.map { list -> list.find { it.videoId == id }?.rating ?: VideoRating.NONE }

    override fun observeRatedVideos(): Flow<List<RatedVideo>> = ratedVideos

    override suspend fun saveRating(
        video: Video,
        rating: VideoRating,
    ) {
        ratedVideos.update { list ->
            val others = list.filterNot { it.videoId == video.id }
            if (rating == VideoRating.NONE) others else others + RatedVideo(video.id, video.channelId, rating)
        }
    }

    override suspend fun fetchRating(
        accessToken: String,
        id: VideoId,
    ): AppResult<VideoRating, DataError> =
        failure(accessToken) ?: AppResult.Success(remoteRatings[id] ?: VideoRating.NONE)

    override suspend fun sendRating(
        accessToken: String,
        id: VideoId,
        rating: VideoRating,
    ): AppResult<Unit, DataError> {
        failure(accessToken)?.let { return it }
        remoteRatings[id] = rating
        return AppResult.Success(Unit)
    }

    private fun failure(accessToken: String): AppResult.Failure<DataError>? =
        (remoteError ?: DataError.Unauthorized.takeIf { accessToken != validToken })?.let { AppResult.Failure(it) }
}
