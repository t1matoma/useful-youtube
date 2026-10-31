package dev.satoshi.usefulyoutube.core.domain.usecase

import assertk.assertThat
import assertk.assertions.isEqualTo
import dev.satoshi.usefulyoutube.core.domain.shorts.ShortsCandidate
import dev.satoshi.usefulyoutube.core.domain.shorts.ShortsVerdict
import dev.satoshi.usefulyoutube.core.model.VideoContentKind
import dev.satoshi.usefulyoutube.core.model.VideoId
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

class ClassifyVideoUseCaseTest {
    private val candidate = ShortsCandidate(VideoId("abc"), 1.minutes)

    private suspend fun classify(
        verdict: ShortsVerdict,
        target: ShortsCandidate = candidate,
    ) = ClassifyVideoUseCase { verdict }(target)

    @Test
    fun `verdicts map to content kinds`() =
        runTest {
            assertThat(classify(ShortsVerdict.SHORT)).isEqualTo(VideoContentKind.SHORT)
            assertThat(classify(ShortsVerdict.NOT_SHORT)).isEqualTo(VideoContentKind.REGULAR)
            assertThat(classify(ShortsVerdict.UNKNOWN)).isEqualTo(VideoContentKind.UNCLASSIFIED)
        }

    @Test
    fun `zero duration is a live stream or premiere`() =
        runTest {
            val kind = classify(ShortsVerdict.SHORT, candidate.copy(duration = Duration.ZERO))

            assertThat(kind).isEqualTo(VideoContentKind.LIVE_OR_PREMIERE)
        }
}
