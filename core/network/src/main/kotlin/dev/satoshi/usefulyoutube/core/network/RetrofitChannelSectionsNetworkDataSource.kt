package dev.satoshi.usefulyoutube.core.network

import dev.satoshi.usefulyoutube.core.model.AppResult
import dev.satoshi.usefulyoutube.core.model.DataError
import dev.satoshi.usefulyoutube.core.network.api.YouTubeDataApi
import kotlinx.serialization.json.Json
import javax.inject.Inject

internal class RetrofitChannelSectionsNetworkDataSource
    @Inject
    constructor(
        private val api: YouTubeDataApi,
        json: Json,
    ) : ChannelSectionsNetworkDataSource {
        private val errorMapper = NetworkErrorMapper(json)

        override suspend fun getFeaturedChannelIds(channelId: String): AppResult<List<String>, DataError> =
            errorMapper.catching {
                AppResult.Success(
                    api
                        .getChannelSections(channelId)
                        .items
                        .filter { it.snippet.type.equals(FEATURED_CHANNELS_SECTION, ignoreCase = true) }
                        .flatMap { it.contentDetails?.channels.orEmpty() }
                        .distinct(),
                )
            }

        private companion object {
            const val FEATURED_CHANNELS_SECTION = "multipleChannels"
        }
    }
