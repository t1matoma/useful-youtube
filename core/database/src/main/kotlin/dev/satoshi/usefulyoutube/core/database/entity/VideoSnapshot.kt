package dev.satoshi.usefulyoutube.core.database.entity

import androidx.room.ColumnInfo
import dev.satoshi.usefulyoutube.core.model.VideoContentKind

/**
 * Everything needed to show and play a video of any channel, embedded into tables that keep videos
 * independently of the subscriptions.
 */
data class VideoSnapshot(
    @ColumnInfo(name = "video_id") val videoId: String,
    @ColumnInfo(name = "channel_id") val channelId: String,
    @ColumnInfo(name = "channel_title") val channelTitle: String,
    val title: String,
    val description: String,
    @ColumnInfo(name = "published_at") val publishedAtEpochMillis: Long,
    @ColumnInfo(name = "duration_seconds") val durationSeconds: Long?,
    @ColumnInfo(name = "thumbnail_url") val thumbnailUrl: String,
    @ColumnInfo(name = "content_kind") val contentKind: VideoContentKind,
)
