package dev.satoshi.usefulyoutube.core.model

import kotlin.time.Instant

/** An entry of the local watch history; [watchedAt] is when the video was last played. */
data class WatchedVideo(
    val video: Video,
    val watchedAt: Instant,
)
