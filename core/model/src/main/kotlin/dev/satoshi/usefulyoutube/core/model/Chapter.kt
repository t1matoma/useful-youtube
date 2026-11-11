package dev.satoshi.usefulyoutube.core.model

import kotlin.time.Duration

/** A part of a video that its author marked with a timestamp in the description. */
data class Chapter(
    val title: String,
    val start: Duration,
)
