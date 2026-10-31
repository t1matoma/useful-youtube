package dev.satoshi.usefulyoutube.core.domain.shorts

import dev.satoshi.usefulyoutube.core.model.VideoId
import kotlin.time.Duration

/** Data needed to decide whether a video is a YouTube Short. */
data class ShortsCandidate(
    val id: VideoId,
    val duration: Duration?,
)

enum class ShortsVerdict {
    SHORT,
    NOT_SHORT,
    UNKNOWN,
}

/** Strategy that decides whether a video is a YouTube Short. */
fun interface ShortsDetector {
    suspend fun detect(candidate: ShortsCandidate): ShortsVerdict
}

/** Live streams and premieres are reported by the Data API with a zero duration (`P0D`). */
internal val ShortsCandidate.isLiveOrPremiere: Boolean
    get() = duration == Duration.ZERO
