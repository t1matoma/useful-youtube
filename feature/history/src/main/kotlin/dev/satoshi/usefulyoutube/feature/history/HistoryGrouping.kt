package dev.satoshi.usefulyoutube.feature.history

import dev.satoshi.usefulyoutube.core.model.WatchedVideo
import java.time.LocalDate
import java.time.ZoneId
import kotlin.time.Instant

/** Groups the history, which is sorted newest first, by the local day the videos were watched. */
internal fun groupByDay(
    history: List<WatchedVideo>,
    now: Instant,
    zone: ZoneId,
): List<HistoryDay> {
    val today = now.localDate(zone)
    return history
        .groupBy { it.watchedAt.localDate(zone) }
        .map { (date, entries) -> HistoryDay(date.label(today), entries.map { it.video }) }
}

private fun Instant.localDate(zone: ZoneId): LocalDate =
    java.time.Instant
        .ofEpochMilli(toEpochMilliseconds())
        .atZone(zone)
        .toLocalDate()

private fun LocalDate.label(today: LocalDate): HistoryDayLabel =
    when (this) {
        today -> HistoryDayLabel.Today
        today.minusDays(1) -> HistoryDayLabel.Yesterday
        else -> HistoryDayLabel.Date(this)
    }
