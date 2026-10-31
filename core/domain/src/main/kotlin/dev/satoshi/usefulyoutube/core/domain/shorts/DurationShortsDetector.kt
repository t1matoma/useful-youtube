package dev.satoshi.usefulyoutube.core.domain.shorts

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** Treats every video not longer than [maximumShortsDuration] as a Short. */
class DurationShortsDetector(
    private val maximumShortsDuration: Duration = DEFAULT_MAXIMUM_SHORTS_DURATION,
) : ShortsDetector {
    override suspend fun detect(candidate: ShortsCandidate): ShortsVerdict {
        val duration = candidate.duration
        return when {
            duration == null -> ShortsVerdict.UNKNOWN
            candidate.isLiveOrPremiere -> ShortsVerdict.NOT_SHORT
            duration <= maximumShortsDuration -> ShortsVerdict.SHORT
            else -> ShortsVerdict.NOT_SHORT
        }
    }

    companion object {
        val DEFAULT_MAXIMUM_SHORTS_DURATION: Duration = 180.seconds
    }
}
