package dev.satoshi.usefulyoutube.core.data.repository

import dev.satoshi.usefulyoutube.core.data.di.IoDispatcher
import dev.satoshi.usefulyoutube.core.data.mapper.toApiValue
import dev.satoshi.usefulyoutube.core.data.mapper.toRatedVideo
import dev.satoshi.usefulyoutube.core.data.mapper.toVideoRating
import dev.satoshi.usefulyoutube.core.database.dao.VideoRatingDao
import dev.satoshi.usefulyoutube.core.database.entity.VideoRatingEntity
import dev.satoshi.usefulyoutube.core.domain.repository.VideoRatingRepository
import dev.satoshi.usefulyoutube.core.model.AppResult
import dev.satoshi.usefulyoutube.core.model.DataError
import dev.satoshi.usefulyoutube.core.model.RatedVideo
import dev.satoshi.usefulyoutube.core.model.Video
import dev.satoshi.usefulyoutube.core.model.VideoId
import dev.satoshi.usefulyoutube.core.model.VideoRating
import dev.satoshi.usefulyoutube.core.model.map
import dev.satoshi.usefulyoutube.core.network.VideoRatingNetworkDataSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** The UI reads the stored ratings; the account is asked and changed only on request. */
internal class OfflineFirstVideoRatingRepository
    @Inject
    constructor(
        private val ratingDao: VideoRatingDao,
        private val networkDataSource: VideoRatingNetworkDataSource,
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : VideoRatingRepository {
        override fun observeRating(id: VideoId): Flow<VideoRating> =
            ratingDao.observeRating(id.value).map { it?.rating ?: VideoRating.NONE }

        override fun observeRatedVideos(): Flow<List<RatedVideo>> =
            ratingDao.observeRatings().map { ratings -> ratings.map { it.toRatedVideo() } }

        override suspend fun saveRating(
            video: Video,
            rating: VideoRating,
        ) = if (rating == VideoRating.NONE) {
            ratingDao.delete(video.id.value)
        } else {
            ratingDao.upsert(VideoRatingEntity(video.id.value, video.channelId.value, rating))
        }

        override suspend fun fetchRating(
            accessToken: String,
            id: VideoId,
        ): AppResult<VideoRating, DataError> =
            withContext(ioDispatcher) {
                networkDataSource.getVideoRating(accessToken, id.value).map { it.toVideoRating() }
            }

        override suspend fun sendRating(
            accessToken: String,
            id: VideoId,
            rating: VideoRating,
        ): AppResult<Unit, DataError> =
            withContext(ioDispatcher) { networkDataSource.rateVideo(accessToken, id.value, rating.toApiValue()) }
    }
