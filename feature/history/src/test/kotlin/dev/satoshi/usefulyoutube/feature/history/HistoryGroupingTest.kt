package dev.satoshi.usefulyoutube.feature.history

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import dev.satoshi.usefulyoutube.core.model.WatchedVideo
import dev.satoshi.usefulyoutube.core.testing.data.TestData
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import kotlin.time.Instant

class HistoryGroupingTest {
    private val zone = ZoneId.of("Europe/Berlin")
    private val now = Instant.parse("2026-11-07T10:00:00Z")

    private fun watched(
        id: String,
        at: String,
    ) = WatchedVideo(TestData.video(id), Instant.parse(at))

    @Test
    fun `history is grouped into today, yesterday and older dates in the local zone`() {
        val history =
            listOf(
                watched("today", "2026-11-07T08:00:00Z"),
                // 00:30 in Berlin, so still today there although it is the 6th in UTC.
                watched("after-midnight", "2026-11-06T23:30:00Z"),
                watched("yesterday", "2026-11-06T12:00:00Z"),
                watched("older-1", "2026-11-03T20:00:00Z"),
                watched("older-2", "2026-11-03T07:00:00Z"),
            )

        val days = groupByDay(history, now, zone)

        assertThat(days.map { it.day }).containsExactly(
            HistoryDayLabel.Today,
            HistoryDayLabel.Yesterday,
            HistoryDayLabel.Date(LocalDate.of(2026, 11, 3)),
        )
        assertThat(days.map { day -> day.videos.map { it.id.value } }).containsExactly(
            listOf("today", "after-midnight"),
            listOf("yesterday"),
            listOf("older-1", "older-2"),
        )
    }

    @Test
    fun `empty history has no days`() {
        assertThat(groupByDay(emptyList(), now, zone)).isEmpty()
    }
}
