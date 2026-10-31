package dev.satoshi.usefulyoutube.core.domain.shorts

import dev.satoshi.usefulyoutube.core.model.AppResult
import dev.satoshi.usefulyoutube.core.model.DataError
import dev.satoshi.usefulyoutube.core.model.VideoId

/** Checks whether `https://www.youtube.com/shorts/{id}` opens without a redirect. */
fun interface ShortsUrlProbe {
    suspend fun isShortsUrl(id: VideoId): AppResult<Boolean, DataError>
}
