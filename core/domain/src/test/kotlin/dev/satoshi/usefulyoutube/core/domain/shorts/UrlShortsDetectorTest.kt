package dev.satoshi.usefulyoutube.core.domain.shorts

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import dev.satoshi.usefulyoutube.core.model.AppResult
import dev.satoshi.usefulyoutube.core.model.DataError
import dev.satoshi.usefulyoutube.core.model.VideoId
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class UrlShortsDetectorTest {
    private val probedIds = mutableListOf<VideoId>()

    private fun detectorReturning(result: AppResult<Boolean, DataError>) =
        UrlShortsDetector { id ->
            probedIds += id
            result
        }

    private val candidate = ShortsCandidate(VideoId("abc"), 30.seconds)

    @Test
    fun `shorts url without redirect means short`() =
        runTest {
            assertThat(detectorReturning(AppResult.Success(true)).detect(candidate)).isEqualTo(ShortsVerdict.SHORT)
            assertThat(probedIds).isEqualTo(listOf(VideoId("abc")))
        }

    @Test
    fun `redirected shorts url means not short`() =
        runTest {
            assertThat(detectorReturning(AppResult.Success(false)).detect(candidate)).isEqualTo(ShortsVerdict.NOT_SHORT)
        }

    @Test
    fun `failed request gives unknown verdict`() =
        runTest {
            val detector = detectorReturning(AppResult.Failure(DataError.NoNetwork))

            assertThat(detector.detect(candidate)).isEqualTo(ShortsVerdict.UNKNOWN)
        }

    @Test
    fun `live stream is not a short and is not probed`() =
        runTest {
            val detector = detectorReturning(AppResult.Success(true))

            assertThat(detector.detect(candidate.copy(duration = Duration.ZERO))).isEqualTo(ShortsVerdict.NOT_SHORT)
            assertThat(probedIds).isEmpty()
        }
}
