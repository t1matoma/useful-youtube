package dev.satoshi.usefulyoutube.core.network

import dev.satoshi.usefulyoutube.core.model.AppResult
import dev.satoshi.usefulyoutube.core.model.DataError
import dev.satoshi.usefulyoutube.core.network.api.YouTubeDataApi
import kotlinx.serialization.json.Json
import javax.inject.Inject

internal class RetrofitVideoRatingNetworkDataSource
    @Inject
    constructor(
        private val api: YouTubeDataApi,
        json: Json,
    ) : VideoRatingNetworkDataSource {
        private val errorMapper = NetworkErrorMapper(json)

        override suspend fun getVideoRating(
            accessToken: String,
            videoId: String,
        ): AppResult<String, DataError> =
            errorMapper.catching {
                val rating =
                    api
                        .getVideoRating("Bearer $accessToken", videoId)
                        .items
                        .firstOrNull { it.videoId == videoId }
                        ?.rating
                AppResult.Success(rating ?: VideoRatingNetworkDataSource.RATING_NONE)
            }

        override suspend fun rateVideo(
            accessToken: String,
            videoId: String,
            rating: String,
        ): AppResult<Unit, DataError> =
            errorMapper.catching {
                api.rateVideo("Bearer $accessToken", videoId, rating)
                AppResult.Success(Unit)
            }
    }
