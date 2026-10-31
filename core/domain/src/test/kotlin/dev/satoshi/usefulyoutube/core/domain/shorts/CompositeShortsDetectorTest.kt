package dev.satoshi.usefulyoutube.core.domain.shorts

import assertk.assertThat
import assertk.assertions.isEqualTo
import dev.satoshi.usefulyoutube.core.model.VideoId
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class CompositeShortsDetectorTest {
    private var confirmationCalls = 0

    private fun composite(
        screeningVerdict: ShortsVerdict,
        confirmationVerdict: ShortsVerdict,
    ) = CompositeShortsDetector(
        screening = { screeningVerdict },
        confirmation = {
            confirmationCalls++
            confirmationVerdict
        },
    )

    private val candidate = ShortsCandidate(VideoId("abc"), 30.seconds)

    @Test
    fun `not short by screening is final and skips confirmation`() =
        runTest {
            val verdict = composite(ShortsVerdict.NOT_SHORT, ShortsVerdict.SHORT).detect(candidate)

            assertThat(verdict).isEqualTo(ShortsVerdict.NOT_SHORT)
            assertThat(confirmationCalls).isEqualTo(0)
        }

    @Test
    fun `unknown by screening skips confirmation`() =
        runTest {
            val verdict = composite(ShortsVerdict.UNKNOWN, ShortsVerdict.SHORT).detect(candidate)

            assertThat(verdict).isEqualTo(ShortsVerdict.UNKNOWN)
            assertThat(confirmationCalls).isEqualTo(0)
        }

    @Test
    fun `confirmation overrides short screening verdict`() =
        runTest {
            val verdict = composite(ShortsVerdict.SHORT, ShortsVerdict.NOT_SHORT).detect(candidate)

            assertThat(verdict).isEqualTo(ShortsVerdict.NOT_SHORT)
        }

    @Test
    fun `confirmed short stays short`() =
        runTest {
            assertThat(
                composite(ShortsVerdict.SHORT, ShortsVerdict.SHORT).detect(candidate),
            ).isEqualTo(ShortsVerdict.SHORT)
        }

    @Test
    fun `unknown confirmation falls back to screening verdict`() =
        runTest {
            assertThat(
                composite(ShortsVerdict.SHORT, ShortsVerdict.UNKNOWN).detect(candidate),
            ).isEqualTo(ShortsVerdict.SHORT)
        }
}
