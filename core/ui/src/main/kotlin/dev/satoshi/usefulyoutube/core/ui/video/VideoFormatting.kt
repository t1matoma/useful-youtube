package dev.satoshi.usefulyoutube.core.ui.video

import kotlin.time.Duration

private const val SECONDS_PER_MINUTE = 60
private const val MINUTES_PER_HOUR = 60

/** Formats a video duration the way YouTube does: `4:05`, `1:02:03`. */
fun formatDuration(duration: Duration): String {
    val totalSeconds = duration.inWholeSeconds
    val hours = totalSeconds / (SECONDS_PER_MINUTE * MINUTES_PER_HOUR)
    val minutes = totalSeconds / SECONDS_PER_MINUTE % MINUTES_PER_HOUR
    val seconds = totalSeconds % SECONDS_PER_MINUTE
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}
