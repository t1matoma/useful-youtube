package dev.satoshi.usefulyoutube.core.data.shorts

import dev.satoshi.usefulyoutube.core.domain.shorts.ShortsUrlProbe
import dev.satoshi.usefulyoutube.core.model.AppResult
import dev.satoshi.usefulyoutube.core.model.DataError
import dev.satoshi.usefulyoutube.core.model.VideoId
import dev.satoshi.usefulyoutube.core.network.YouTubeNetworkDataSource
import javax.inject.Inject

internal class NetworkShortsUrlProbe
    @Inject
    constructor(
        private val networkDataSource: YouTubeNetworkDataSource,
    ) : ShortsUrlProbe {
        override suspend fun isShortsUrl(id: VideoId): AppResult<Boolean, DataError> =
            networkDataSource.isShortsUrl(id.value)
    }
