package dev.satoshi.usefulyoutube.core.domain.repository

import dev.satoshi.usefulyoutube.core.model.AppResult
import dev.satoshi.usefulyoutube.core.model.Channel
import dev.satoshi.usefulyoutube.core.model.ImportError

/** Reads the `subscriptions.csv` file exported by Google Takeout. */
fun interface SubscriptionsFileReader {
    suspend fun read(fileUri: String): AppResult<List<Channel>, ImportError>
}
