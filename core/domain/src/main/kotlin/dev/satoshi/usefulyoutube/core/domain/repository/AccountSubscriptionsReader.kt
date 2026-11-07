package dev.satoshi.usefulyoutube.core.domain.repository

import dev.satoshi.usefulyoutube.core.model.AppResult
import dev.satoshi.usefulyoutube.core.model.Channel
import dev.satoshi.usefulyoutube.core.model.DataError

/** Reads all subscriptions of the authorized YouTube account. */
fun interface AccountSubscriptionsReader {
    suspend fun read(accessToken: String): AppResult<List<Channel>, DataError>
}
