package dev.satoshi.usefulyoutube.core.data.mapper

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import dev.satoshi.usefulyoutube.core.data.testdoubles.FakeYouTubeNetworkDataSource.Companion.details
import dev.satoshi.usefulyoutube.core.data.testdoubles.FakeYouTubeNetworkDataSource.Companion.entry
import dev.satoshi.usefulyoutube.core.model.VideoContentKind
import dev.satoshi.usefulyoutube.core.network.model.ThumbnailDto
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class VideoMappersTest {
    @Test
    fun `iso durations from the data api are parsed`() {
        assertThat(parseIsoDuration("PT4M13S")).isEqualTo(4.minutes + 13.seconds)
        assertThat(parseIsoDuration("PT1H2M3S")).isEqualTo(1.hours + 2.minutes + 3.seconds)
        assertThat(parseIsoDuration("P1DT2H")).isEqualTo(1.days + 2.hours)
        assertThat(parseIsoDuration("PT59S")).isEqualTo(59.seconds)
        assertThat(parseIsoDuration("P0D")).isEqualTo(Duration.ZERO)
        assertThat(parseIsoDuration("garbage")).isNull()
    }

    @Test
    fun `rss entry becomes unclassified video without details`() {
        val entity = entry("v1", "UCchannel", published = "2026-10-30T19:00:06+03:00").toVideoEntity("UCchannel")

        assertThat(entity?.publishedAtEpochMillis).isEqualTo(1_793_376_006_000L)
        assertThat(entity?.contentKind).isEqualTo(VideoContentKind.UNCLASSIFIED)
        assertThat(entity?.detailsFetched).isEqualTo(false)
        assertThat(entity?.durationSeconds).isNull()
    }

    @Test
    fun `rss entry without media group gets default thumbnail and empty description`() {
        val entity = entry("v1", "UCchannel").copy(mediaGroup = null).toVideoEntity("UCchannel")

        assertThat(entity?.thumbnailUrl).isEqualTo("https://i.ytimg.com/vi/v1/hqdefault.jpg")
        assertThat(entity?.description).isEqualTo("")
    }

    @Test
    fun `details pick the best available thumbnail`() {
        val item = details("v1", "UCchannel", duration = "PT5M")
        val withThumbnails =
            item.copy(
                snippet =
                    item.snippet.copy(
                        thumbnails = mapOf("default" to ThumbnailDto("small"), "high" to ThumbnailDto("large")),
                    ),
            )

        assertThat(withThumbnails.toDetailsUpdate().thumbnailUrl).isEqualTo("large")
        assertThat(item.toDetailsUpdate().thumbnailUrl).isEqualTo("https://i.ytimg.com/vi/v1/hqdefault.jpg")
        assertThat(item.toDetailsUpdate().durationSeconds).isEqualTo(300L)
    }

    @Test
    fun `details without duration keep it unknown`() {
        val item = details("v1", "UCchannel", duration = null)

        assertThat(item.toDetailsUpdate().durationSeconds).isNull()
    }
}
