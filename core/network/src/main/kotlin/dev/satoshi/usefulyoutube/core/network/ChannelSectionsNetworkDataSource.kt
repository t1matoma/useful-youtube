package dev.satoshi.usefulyoutube.core.network

import dev.satoshi.usefulyoutube.core.model.AppResult
import dev.satoshi.usefulyoutube.core.model.DataError

/** The sections of a channel page, through the Data API. */
fun interface ChannelSectionsNetworkDataSource {
    /** Ids of the channels that [channelId] features on its page, without duplicates. */
    suspend fun getFeaturedChannelIds(channelId: String): AppResult<List<String>, DataError>
}
