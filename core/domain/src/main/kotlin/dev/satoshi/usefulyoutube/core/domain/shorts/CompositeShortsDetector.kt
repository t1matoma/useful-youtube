package dev.satoshi.usefulyoutube.core.domain.shorts

/**
 * Combines a cheap [screening] strategy with an expensive [confirmation] strategy.
 *
 * Only videos that [screening] marks as Shorts are checked by [confirmation]. When the confirmation
 * cannot decide, the screening verdict is kept.
 */
class CompositeShortsDetector(
    private val screening: ShortsDetector,
    private val confirmation: ShortsDetector,
) : ShortsDetector {
    override suspend fun detect(candidate: ShortsCandidate): ShortsVerdict {
        val screeningVerdict = screening.detect(candidate)
        if (screeningVerdict != ShortsVerdict.SHORT) return screeningVerdict
        val confirmationVerdict = confirmation.detect(candidate)
        return if (confirmationVerdict == ShortsVerdict.UNKNOWN) screeningVerdict else confirmationVerdict
    }
}
