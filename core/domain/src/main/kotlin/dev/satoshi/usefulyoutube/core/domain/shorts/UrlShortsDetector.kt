package dev.satoshi.usefulyoutube.core.domain.shorts

import dev.satoshi.usefulyoutube.core.model.AppResult

/** Asks YouTube whether the video is available under the `/shorts/` URL. */
class UrlShortsDetector(
    private val probe: ShortsUrlProbe,
) : ShortsDetector {
    override suspend fun detect(candidate: ShortsCandidate): ShortsVerdict {
        if (candidate.isLiveOrPremiere) return ShortsVerdict.NOT_SHORT
        return when (val result = probe.isShortsUrl(candidate.id)) {
            is AppResult.Success -> if (result.value) ShortsVerdict.SHORT else ShortsVerdict.NOT_SHORT
            is AppResult.Failure -> ShortsVerdict.UNKNOWN
        }
    }
}
