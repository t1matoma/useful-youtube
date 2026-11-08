package dev.satoshi.usefulyoutube.core.model

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** Where the playback of a video stopped; [duration] is the length of the video reported by the player. */
data class PlaybackPosition(
    val position: Duration,
    val duration: Duration,
) {
    /** The watched part of the video, from 0 to 1, for a progress bar. */
    val progress: Float
        get() = if (duration.isPositive()) (position / duration).toFloat().coerceIn(0f, 1f) else 0f

    /** The video was watched to less than [END_MARGIN] before its end. */
    val isFinished: Boolean
        get() = duration.isPositive() && duration - position < END_MARGIN

    companion object {
        val END_MARGIN = 10.seconds
    }
}
