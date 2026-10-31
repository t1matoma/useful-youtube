package dev.satoshi.usefulyoutube.core.model

import kotlin.time.Duration
import kotlin.time.Instant

data class Video(
    val id: VideoId,
    val channelId: ChannelId,
    val channelTitle: String,
    val title: String,
    val description: String,
    val publishedAt: Instant,
    val duration: Duration?,
    val thumbnailUrl: String,
    val contentKind: VideoContentKind,
)
