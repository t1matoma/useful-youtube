package dev.satoshi.usefulyoutube.core.domain.usecase

import dev.satoshi.usefulyoutube.core.domain.shorts.ShortsCandidate
import dev.satoshi.usefulyoutube.core.domain.shorts.ShortsDetector
import dev.satoshi.usefulyoutube.core.domain.shorts.ShortsVerdict
import dev.satoshi.usefulyoutube.core.model.VideoContentKind
import javax.inject.Inject
import kotlin.time.Duration

class ClassifyVideoUseCase
    @Inject
    constructor(
        private val shortsDetector: ShortsDetector,
    ) {
        suspend operator fun invoke(candidate: ShortsCandidate): VideoContentKind {
            if (candidate.duration == Duration.ZERO) return VideoContentKind.LIVE_OR_PREMIERE
            return when (shortsDetector.detect(candidate)) {
                ShortsVerdict.SHORT -> VideoContentKind.SHORT
                ShortsVerdict.NOT_SHORT -> VideoContentKind.REGULAR
                ShortsVerdict.UNKNOWN -> VideoContentKind.UNCLASSIFIED
            }
        }
    }
