package dev.satoshi.usefulyoutube.feature.feed.component

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class VideoFormattingTest {
    @Test
    fun `formats durations like youtube`() {
        assertThat(formatDuration(Duration.ZERO)).isEqualTo("0:00")
        assertThat(formatDuration(5.seconds)).isEqualTo("0:05")
        assertThat(formatDuration(4.minutes + 5.seconds)).isEqualTo("4:05")
        assertThat(formatDuration(59.minutes + 59.seconds)).isEqualTo("59:59")
        assertThat(formatDuration(1.hours + 2.minutes + 3.seconds)).isEqualTo("1:02:03")
        assertThat(formatDuration(12.hours)).isEqualTo("12:00:00")
    }
}
