package dev.satoshi.usefulyoutube.core.data.testdoubles

import dev.satoshi.usefulyoutube.core.model.AppResult
import dev.satoshi.usefulyoutube.core.model.DataError
import dev.satoshi.usefulyoutube.core.network.ChannelSectionsNetworkDataSource
import dev.satoshi.usefulyoutube.core.network.VideoRatingNetworkDataSource

class FakeVideoRatingNetworkDataSource : VideoRatingNetworkDataSource {
    val ratings = mutableMapOf<String, String>()
    var error: DataError? = null
    val usedTokens = mutableListOf<String>()

    override suspend fun getVideoRating(
        accessToken: String,
        videoId: String,
    ): AppResult<String, DataError> {
        usedTokens += accessToken
        error?.let { return AppResult.Failure(it) }
        return AppResult.Success(ratings[videoId] ?: "unspecified")
    }

    override suspend fun rateVideo(
        accessToken: String,
        videoId: String,
        rating: String,
    ): AppResult<Unit, DataError> {
        usedTokens += accessToken
        error?.let { return AppResult.Failure(it) }
        ratings[videoId] = rating
        return AppResult.Success(Unit)
    }
}

class FakeChannelSectionsNetworkDataSource : ChannelSectionsNetworkDataSource {
    val featured = mutableMapOf<String, List<String>>()
    var error: DataError? = null
    val requests = mutableListOf<String>()

    override suspend fun getFeaturedChannelIds(channelId: String): AppResult<List<String>, DataError> {
        requests += channelId
        error?.let { return AppResult.Failure(it) }
        return AppResult.Success(featured[channelId].orEmpty())
    }
}
