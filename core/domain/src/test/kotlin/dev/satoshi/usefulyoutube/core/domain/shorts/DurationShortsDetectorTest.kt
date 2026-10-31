package dev.satoshi.usefulyoutube.core.domain.shorts

import assertk.assertThat
import assertk.assertions.isEqualTo
import dev.satoshi.usefulyoutube.core.model.VideoId
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class DurationShortsDetectorTest {
    private val detector = DurationShortsDetector()

    private suspend fun verdictFor(duration: Duration?) = detector.detect(ShortsCandidate(VideoId("id"), duration))

    @Test
    fun `video shorter than three minutes is a short`() =
        runTest {
            assertThat(verdictFor(45.seconds)).isEqualTo(ShortsVerdict.SHORT)
        }

    @Test
    fun `video of exactly three minutes is a short`() =
        runTest {
            assertThat(verdictFor(180.seconds)).isEqualTo(ShortsVerdict.SHORT)
        }

    @Test
    fun `video longer than three minutes is not a short`() =
        runTest {
            assertThat(verdictFor(181.seconds)).isEqualTo(ShortsVerdict.NOT_SHORT)
            assertThat(verdictFor(42.minutes)).isEqualTo(ShortsVerdict.NOT_SHORT)
        }

    @Test
    fun `live stream or premiere with zero duration is not a short`() =
        runTest {
            assertThat(verdictFor(Duration.ZERO)).isEqualTo(ShortsVerdict.NOT_SHORT)
        }

    @Test
    fun `unknown duration gives unknown verdict`() =
        runTest {
            assertThat(verdictFor(null)).isEqualTo(ShortsVerdict.UNKNOWN)
        }

    @Test
    fun `custom threshold is respected`() =
        runTest {
            val customDetector = DurationShortsDetector(maximumShortsDuration = 60.seconds)

            assertThat(
                customDetector.detect(ShortsCandidate(VideoId("id"), 90.seconds)),
            ).isEqualTo(ShortsVerdict.NOT_SHORT)
        }
}
